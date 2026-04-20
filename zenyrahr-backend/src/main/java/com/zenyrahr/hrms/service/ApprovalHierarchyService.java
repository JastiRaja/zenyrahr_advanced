package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.ApprovalHierarchyRuleRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.dto.ApprovalHierarchyModuleDTO;
import com.zenyrahr.hrms.dto.ApprovalHierarchyRequestDTO;
import com.zenyrahr.hrms.dto.ApprovalHierarchyStepDTO;
import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ApprovalHierarchyService {
    private static final String REQUESTER_ROLE_ALL = "all";
    private final ApprovalHierarchyRuleRepository ruleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final OrganizationRoleService organizationRoleService;

    public List<ApprovalHierarchyModuleDTO> getCurrentHierarchy(Employee actor) {
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        return List.of(
                toModuleDto(orgId, ApprovalModule.LEAVE),
                toModuleDto(orgId, ApprovalModule.TIMESHEET),
                toModuleDto(orgId, ApprovalModule.TRAVEL),
                toModuleDto(orgId, ApprovalModule.EXPENSE)
        );
    }

    @Transactional
    public List<ApprovalHierarchyModuleDTO> saveCurrentHierarchy(Employee actor, ApprovalHierarchyRequestDTO payload) {
        if (!(tenantAccessService.isOrgAdmin(actor) || tenantAccessService.isMainAdmin(actor))) {
            throw new ResponseStatusException(FORBIDDEN, "Only organization admin can configure approval hierarchy");
        }
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        Map<ApprovalModule, List<ApprovalHierarchyStepDTO>> incoming = normalizePayload(payload);
        for (ApprovalModule module : ApprovalModule.values()) {
            List<ApprovalHierarchyStepDTO> steps = incoming.getOrDefault(module, defaultSteps(module));
            validateSteps(module, orgId, steps);
            ruleRepository.deleteByOrganization_IdAndModule(orgId, module);
            List<ApprovalHierarchyRule> rows = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                ApprovalHierarchyStepDTO step = steps.get(i);
                ApprovalHierarchyRule rule = new ApprovalHierarchyRule();
                rule.setOrganization(organization);
                rule.setModule(module);
                rule.setLevelNo(i + 1);
                rule.setRequesterRole(resolveRequesterRoleForRule(module, step.getRequesterRole()));
                rule.setApproverType(step.getApproverType());
                rule.setApproverRole(step.getApproverRole() == null ? null : step.getApproverRole().toLowerCase(Locale.ROOT));
                if (step.getApproverUserId() != null) {
                    Employee approver = userRepository.findById(step.getApproverUserId())
                            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Approver user not found"));
                    Long approverOrgId = approver.getOrganization() != null ? approver.getOrganization().getId() : null;
                    if (!Objects.equals(approverOrgId, orgId)) {
                        throw new ResponseStatusException(BAD_REQUEST, "Approver user must belong to the same organization");
                    }
                    rule.setApproverUser(approver);
                }
                rule.setActive(true);
                rows.add(rule);
            }
            ruleRepository.saveAll(rows);
        }
        return getCurrentHierarchy(actor);
    }

    public void assertActorCanApprove(Employee actor, Employee targetEmployee, ApprovalModule module, int levelNo) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        Long targetOrgId = targetEmployee != null && targetEmployee.getOrganization() != null
                ? targetEmployee.getOrganization().getId()
                : null;
        if (!Objects.equals(orgId, targetOrgId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        String requesterRole = resolveRequesterRoleForLookup(module, targetEmployee == null ? null : targetEmployee.getRole());
        ApprovalHierarchyStepDTO step = module == ApprovalModule.LEAVE
                ? effectiveSteps(orgId, module).stream()
                        .filter(s -> Objects.equals(normalizeRole(s.getRequesterRole()), requesterRole))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "No leave approver configured for requester role: " + requesterRole))
                : effectiveSteps(orgId, module).stream()
                        .filter(s -> Objects.equals(s.getLevelNo(), levelNo))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "No approval rule configured for requested level"));

        boolean allowed;
        switch (step.getApproverType()) {
            case REPORTING_MANAGER -> {
                Employee rm = targetEmployee.getReportingManager();
                allowed = rm != null && Objects.equals(rm.getId(), actor.getId());
            }
            case ROLE -> {
                String actorRole = actor.getRole() == null ? "" : actor.getRole().toLowerCase(Locale.ROOT);
                String expectedRole = step.getApproverRole() == null ? "" : step.getApproverRole().toLowerCase(Locale.ROOT);
                allowed = !expectedRole.isBlank() && expectedRole.equals(actorRole);
            }
            case SPECIFIC_USER -> allowed = Objects.equals(step.getApproverUserId(), actor.getId());
            default -> allowed = false;
        }
        if (!allowed) {
            throw new ResponseStatusException(FORBIDDEN, "You are not configured as approver for this approval level");
        }
    }

    public int getRequiredApprovalLevels(Employee targetEmployee, ApprovalModule module) {
        if (module == ApprovalModule.LEAVE) {
            return 1;
        }
        if (targetEmployee == null || targetEmployee.getOrganization() == null || targetEmployee.getOrganization().getId() == null) {
            return 1;
        }
        Long orgId = targetEmployee.getOrganization().getId();
        int levels = effectiveSteps(orgId, module).size();
        return Math.max(1, levels);
    }

    private ApprovalHierarchyModuleDTO toModuleDto(Long orgId, ApprovalModule module) {
        ApprovalHierarchyModuleDTO dto = new ApprovalHierarchyModuleDTO();
        dto.setModule(module);
        dto.setSteps(effectiveSteps(orgId, module));
        return dto;
    }

    private List<ApprovalHierarchyStepDTO> effectiveSteps(Long orgId, ApprovalModule module) {
        List<ApprovalHierarchyRule> configured = ruleRepository.findByOrganization_IdAndModuleAndActiveTrueOrderByLevelNoAsc(orgId, module);
        if (configured.isEmpty()) {
            return normalizeDeprecatedApproverRoleList(module, defaultSteps(module));
        }
        if (module == ApprovalModule.LEAVE) {
            boolean hasRequesterRoleMapping = configured.stream()
                    .map(ApprovalHierarchyRule::getRequesterRole)
                    .anyMatch(role -> !normalizeRole(role).isBlank() && !REQUESTER_ROLE_ALL.equals(normalizeRole(role)));
            if (!hasRequesterRoleMapping) {
                return normalizeDeprecatedApproverRoleList(module, defaultLeaveSteps());
            }
        }
        return configured.stream()
                .sorted(Comparator.comparing(ApprovalHierarchyRule::getLevelNo))
                .map(this::toStepDto)
                .map(dto -> normalizeDeprecatedApproverRole(module, dto))
                .collect(Collectors.toList());
    }

    /**
     * Older defaults used platform role {@code admin} as a timesheet/travel/expense approver, which
     * incorrectly targeted the main tenant admin. Treat that as {@code org_admin} at read time so
     * existing persisted rules behave correctly without a data migration.
     */
    private ApprovalHierarchyStepDTO normalizeDeprecatedApproverRole(ApprovalModule module, ApprovalHierarchyStepDTO dto) {
        if (dto == null || module == ApprovalModule.LEAVE) {
            return dto;
        }
        if (dto.getApproverType() == ApproverType.ROLE && dto.getApproverRole() != null
                && ("admin".equalsIgnoreCase(dto.getApproverRole())
                || "zenyrahr_admin".equalsIgnoreCase(dto.getApproverRole()))) {
            dto.setApproverRole("org_admin");
        }
        return dto;
    }

    private List<ApprovalHierarchyStepDTO> normalizeDeprecatedApproverRoleList(
            ApprovalModule module,
            List<ApprovalHierarchyStepDTO> steps
    ) {
        return steps.stream()
                .map(dto -> normalizeDeprecatedApproverRole(module, dto))
                .collect(Collectors.toList());
    }

    private ApprovalHierarchyStepDTO toStepDto(ApprovalHierarchyRule rule) {
        ApprovalHierarchyStepDTO dto = new ApprovalHierarchyStepDTO();
        dto.setLevelNo(rule.getLevelNo());
        dto.setRequesterRole(rule.getRequesterRole());
        dto.setApproverType(rule.getApproverType());
        dto.setApproverRole(rule.getApproverRole());
        if (rule.getApproverUser() != null) {
            dto.setApproverUserId(rule.getApproverUser().getId());
            dto.setApproverUserName(rule.getApproverUser().getFirstName() + " " + rule.getApproverUser().getLastName());
        }
        return dto;
    }

    private Map<ApprovalModule, List<ApprovalHierarchyStepDTO>> normalizePayload(ApprovalHierarchyRequestDTO payload) {
        Map<ApprovalModule, List<ApprovalHierarchyStepDTO>> result = new EnumMap<>(ApprovalModule.class);
        if (payload == null || payload.getModules() == null) {
            return result;
        }
        for (ApprovalHierarchyModuleDTO module : payload.getModules()) {
            if (module == null || module.getModule() == null) continue;
            List<ApprovalHierarchyStepDTO> steps = module.getSteps() == null ? List.of() : module.getSteps();
            List<ApprovalHierarchyStepDTO> normalized = steps.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ApprovalHierarchyStepDTO::getLevelNo, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
            result.put(module.getModule(), normalized);
        }
        return result;
    }

    private void validateSteps(ApprovalModule module, Long orgId, List<ApprovalHierarchyStepDTO> steps) {
        if (steps.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, module + " hierarchy must include at least one level");
        }
        List<String> requesterRolesSeen = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ApprovalHierarchyStepDTO step = steps.get(i);
            if (module == ApprovalModule.LEAVE) {
                String requesterRole = normalizeRole(step.getRequesterRole());
                if (requesterRole.isBlank() || REQUESTER_ROLE_ALL.equals(requesterRole)) {
                    throw new ResponseStatusException(BAD_REQUEST, "Requester role is required for LEAVE hierarchy");
                }
                organizationRoleService.assertRoleExistsForOrganization(orgId, requesterRole);
                if (requesterRolesSeen.contains(requesterRole)) {
                    throw new ResponseStatusException(BAD_REQUEST, "Duplicate leave mapping for requester role: " + requesterRole);
                }
                requesterRolesSeen.add(requesterRole);
            } else {
                int expectedLevel = i + 1;
                if (step.getLevelNo() == null || step.getLevelNo() != expectedLevel) {
                    throw new ResponseStatusException(BAD_REQUEST, "Approval levels must be sequential starting from 1");
                }
                step.setRequesterRole(REQUESTER_ROLE_ALL);
            }
            if (step.getApproverType() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Approver type is required for each level");
            }
            if (step.getApproverType() == ApproverType.ROLE) {
                if (step.getApproverRole() == null || step.getApproverRole().isBlank()) {
                    throw new ResponseStatusException(BAD_REQUEST, "Approver role is required for ROLE approver type");
                }
                organizationRoleService.assertRoleExistsForOrganization(orgId, step.getApproverRole());
                if (module == ApprovalModule.LEAVE) {
                    String requesterRole = normalizeRole(step.getRequesterRole());
                    String approverRole = normalizeRole(step.getApproverRole());
                    if (!requesterRole.isBlank() && requesterRole.equals(approverRole)) {
                        throw new ResponseStatusException(BAD_REQUEST, "Requester role and approver role cannot be the same for leave mapping");
                    }
                }
            } else {
                step.setApproverRole(null);
            }
            if (step.getApproverType() == ApproverType.SPECIFIC_USER) {
                if (step.getApproverUserId() == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "Approver user is required for SPECIFIC_USER approver type");
                }
                Employee approver = userRepository.findById(step.getApproverUserId())
                        .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Approver user not found"));
                Long approverOrgId = approver.getOrganization() != null ? approver.getOrganization().getId() : null;
                if (!Objects.equals(approverOrgId, orgId)) {
                    throw new ResponseStatusException(BAD_REQUEST, "Approver user must belong to same organization");
                }
            } else {
                step.setApproverUserId(null);
                step.setApproverUserName(null);
            }
        }
    }

    private List<ApprovalHierarchyStepDTO> defaultSteps(ApprovalModule module) {
        if (module == ApprovalModule.LEAVE) {
            return defaultLeaveSteps();
        }
        ApprovalHierarchyStepDTO level1 = new ApprovalHierarchyStepDTO();
        level1.setLevelNo(1);
        level1.setRequesterRole(REQUESTER_ROLE_ALL);
        level1.setApproverType(ApproverType.REPORTING_MANAGER);

        ApprovalHierarchyStepDTO level2 = new ApprovalHierarchyStepDTO();
        level2.setLevelNo(2);
        level2.setRequesterRole(REQUESTER_ROLE_ALL);
        level2.setApproverType(ApproverType.ROLE);
        // Organization-level final approver — not platform "admin" (main tenant admin).
        level2.setApproverRole("org_admin");
        return List.of(level1, level2);
    }

    private List<ApprovalHierarchyStepDTO> defaultLeaveSteps() {
        List<ApprovalHierarchyStepDTO> defaults = new ArrayList<>();

        ApprovalHierarchyStepDTO employeeRule = new ApprovalHierarchyStepDTO();
        employeeRule.setLevelNo(1);
        employeeRule.setRequesterRole("employee");
        employeeRule.setApproverType(ApproverType.REPORTING_MANAGER);
        defaults.add(employeeRule);

        ApprovalHierarchyStepDTO managerRule = new ApprovalHierarchyStepDTO();
        managerRule.setLevelNo(2);
        managerRule.setRequesterRole("manager");
        managerRule.setApproverType(ApproverType.ROLE);
        managerRule.setApproverRole("hr");
        defaults.add(managerRule);

        ApprovalHierarchyStepDTO hrRule = new ApprovalHierarchyStepDTO();
        hrRule.setLevelNo(3);
        hrRule.setRequesterRole("hr");
        hrRule.setApproverType(ApproverType.ROLE);
        hrRule.setApproverRole("org_admin");
        defaults.add(hrRule);

        return defaults;
    }

    private String resolveRequesterRoleForRule(ApprovalModule module, String requesterRole) {
        if (module != ApprovalModule.LEAVE) {
            return REQUESTER_ROLE_ALL;
        }
        return normalizeRole(requesterRole);
    }

    private String resolveRequesterRoleForLookup(ApprovalModule module, String requesterRole) {
        if (module != ApprovalModule.LEAVE) {
            return REQUESTER_ROLE_ALL;
        }
        return normalizeRole(requesterRole);
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }
}

