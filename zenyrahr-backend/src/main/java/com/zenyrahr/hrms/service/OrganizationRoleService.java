package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.OrganizationRoleRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.model.OrganizationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OrganizationRoleService {

    private static final List<String> DEFAULT_ROLES = List.of("org_admin");
    private static final List<String> NON_DELETABLE_ROLES = List.of("org_admin");
    private static final Set<String> ALLOWED_BASE_SYSTEM_ROLES = Set.of("org_admin", "hr", "manager", "employee");
    private static final Set<String> ALLOWED_CAPABILITY_PACKS =
            Set.of("recruitment", "finance", "compliance", "learning", "engagement");

    private final OrganizationRoleRepository organizationRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final UserRepository userRepository;

    public List<OrganizationRole> getRoles(Long organizationId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanManageEmployees(actor);
        Long scopedOrgId = resolveScopedOrgId(actor, organizationId);
        bootstrapDefaultsIfNeeded(scopedOrgId);
        return organizationRoleRepository.findByOrganization_IdOrderByNameAsc(scopedOrgId);
    }

    public OrganizationRole createRole(Long organizationId, String rawName, String rawBaseSystemRole, List<String> rawCapabilityPacks) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanManageRoleCatalog(actor);
        Long scopedOrgId = resolveScopedOrgId(actor, organizationId);
        String normalized = normalizeRoleName(rawName);
        if (organizationRoleRepository.existsByNameIgnoreCaseAndOrganization_Id(normalized, scopedOrgId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Role already exists in this organization");
        }
        Organization organization = organizationRepository.findById(scopedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        OrganizationRole role = new OrganizationRole();
        role.setName(normalized);
        role.setBaseSystemRole(normalizeBaseSystemRole(rawBaseSystemRole));
        role.setCapabilityPacks(normalizeCapabilityPacks(rawCapabilityPacks));
        role.setOrganization(organization);
        return organizationRoleRepository.save(role);
    }

    public void deleteRole(Long id, Long organizationId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanManageRoleCatalog(actor);
        Long scopedOrgId = resolveScopedOrgId(actor, organizationId);
        OrganizationRole role = organizationRoleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Role not found"));
        Long roleOrgId = role.getOrganization() != null ? role.getOrganization().getId() : null;
        if (!Objects.equals(scopedOrgId, roleOrgId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        if (NON_DELETABLE_ROLES.contains(role.getName().toLowerCase())) {
            throw new ResponseStatusException(BAD_REQUEST, "Role 'org_admin' cannot be deleted");
        }
        if (userRepository.existsByRoleIgnoreCaseAndOrganization_Id(role.getName(), scopedOrgId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot delete role assigned to employees");
        }
        organizationRoleRepository.delete(role);
    }

    private Long resolveScopedOrgId(Employee actor, Long organizationId) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (organizationId == null) {
                throw new ResponseStatusException(BAD_REQUEST, "organizationId is required for main admin");
            }
            return organizationId;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        if (organizationId != null && !Objects.equals(actorOrgId, organizationId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        return actorOrgId;
    }

    private void bootstrapDefaultsIfNeeded(Long organizationId) {
        if (organizationRoleRepository.countByOrganization_Id(organizationId) > 0) {
            return;
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        List<OrganizationRole> defaults = DEFAULT_ROLES.stream().map(name -> {
            OrganizationRole role = new OrganizationRole();
            role.setName(name);
            role.setBaseSystemRole(name);
            role.setCapabilityPacks(List.of());
            role.setOrganization(organization);
            return role;
        }).toList();
        organizationRoleRepository.saveAll(defaults);
    }

    public String normalizeAssignableRole(String roleName) {
        String value = roleName == null ? "" : roleName.trim().toLowerCase();
        if (value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Role is required");
        }
        return value;
    }

    public void assertRoleExistsForOrganization(Long organizationId, String roleName) {
        String normalized = normalizeAssignableRole(roleName);
        if (TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE.equalsIgnoreCase(normalized)) {
            return;
        }
        bootstrapDefaultsIfNeeded(organizationId);
        if (!organizationRoleRepository.existsByNameIgnoreCaseAndOrganization_Id(normalized, organizationId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Role is not available in this organization");
        }
    }

    public List<String> resolveCapabilityPacksForUser(Employee employee) {
        if (employee == null || employee.getRole() == null || tenantAccessService.isMainAdmin(employee)) {
            return List.of();
        }
        Long organizationId = tenantAccessService.requireOrganizationId(employee);
        return organizationRoleRepository
                .findByNameIgnoreCaseAndOrganization_Id(employee.getRole(), organizationId)
                .map(role -> role.getCapabilityPacks() == null ? List.<String>of() : role.getCapabilityPacks())
                .orElse(List.of());
    }

    public String resolveBaseSystemRoleForUser(Employee employee) {
        if (employee == null || employee.getRole() == null) {
            return "employee";
        }
        String role = employee.getRole().trim().toLowerCase(Locale.ROOT);
        if (TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE.equals(role)) {
            return role;
        }
        if (ALLOWED_BASE_SYSTEM_ROLES.contains(role)) {
            return role;
        }
        if (employee.getOrganization() == null || employee.getOrganization().getId() == null) {
            return "employee";
        }
        return organizationRoleRepository
                .findByNameIgnoreCaseAndOrganization_Id(role, employee.getOrganization().getId())
                .map(OrganizationRole::getBaseSystemRole)
                .orElse("employee");
    }

    private void assertCanManageRoleCatalog(Employee actor) {
        if (tenantAccessService.isMainAdmin(actor) || tenantAccessService.isOrgAdmin(actor)) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Only organization admin can manage roles");
    }

    private String normalizeRoleName(String input) {
        String value = input == null ? "" : input.trim().toLowerCase();
        if (value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Role name is required");
        }
        if (!value.matches("^[a-z][a-z0-9_]{1,39}$")) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Role must use lowercase letters, numbers, and underscore only"
            );
        }
        if (TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE.equalsIgnoreCase(value)) {
            throw new ResponseStatusException(BAD_REQUEST, "Role name '" + TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE + "' is reserved");
        }
        return value;
    }

    private String normalizeBaseSystemRole(String input) {
        String normalized = input == null || input.isBlank() ? "employee" : input.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_BASE_SYSTEM_ROLES.contains(normalized)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "baseSystemRole must be one of org_admin, hr, manager, employee"
            );
        }
        return normalized;
    }

    private List<String> normalizeCapabilityPacks(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String pack : input) {
            String value = pack == null ? "" : pack.trim().toLowerCase(Locale.ROOT);
            if (value.isBlank()) {
                continue;
            }
            if (!ALLOWED_CAPABILITY_PACKS.contains(value)) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Unsupported capability pack: " + value
                );
            }
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }
}
