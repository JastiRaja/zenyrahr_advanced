package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeCodeService {
    private static final Pattern MANUAL_CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,31}$");

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    @Transactional
    public String resolveCodeForNewEmployee(Employee actor, Organization employeeOrganization, String requestedCode) {
        if (employeeOrganization == null || employeeOrganization.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Organization is required to generate employee ID");
        }
        Organization org = organizationRepository.findWithLockingById(employeeOrganization.getId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Organization not found"));

        String requested = requestedCode == null ? "" : requestedCode.trim();
        if (!requested.isBlank()) {
            return validateAndUseManualCode(actor, org, requested);
        }
        return generateAutomaticCode(org);
    }

    public void validateSettingsForUpdate(Employee actor, Organization organization, String prefix, Integer padding, Integer nextNumber, Boolean allowManual) {
        if (organization == null || organization.getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Organization not found");
        }
        if (!(tenantAccessService.isMainAdmin(actor) || tenantAccessService.isOrgAdmin(actor))) {
            throw new ResponseStatusException(FORBIDDEN, "Only organization admin can manage employee ID settings");
        }
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            if (!Objects.equals(actorOrgId, organization.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "Cross-organization settings update is not allowed");
            }
        }
        if (prefix == null || prefix.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Employee ID prefix is required");
        }
        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        if (!normalizedPrefix.matches("^[A-Z0-9_-]{1,16}$")) {
            throw new ResponseStatusException(BAD_REQUEST, "Prefix must be 1-16 chars (A-Z, 0-9, _ or -)");
        }
        if (padding == null || padding < 1 || padding > 8) {
            throw new ResponseStatusException(BAD_REQUEST, "Padding must be between 1 and 8");
        }
        if (nextNumber == null || nextNumber < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Next employee code number must be greater than zero");
        }
        if (allowManual == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Manual override flag is required");
        }
    }

    private String validateAndUseManualCode(Employee actor, Organization org, String requested) {
        if (!Boolean.TRUE.equals(org.getAllowManualEmployeeCodeOverride())) {
            throw new ResponseStatusException(BAD_REQUEST, "Manual employee ID override is disabled for this organization");
        }
        if (!tenantAccessService.isOrgAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only organization admin can set manual employee IDs");
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        if (!Objects.equals(actorOrgId, org.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization manual employee ID assignment is not allowed");
        }
        String normalized = requested.toUpperCase(Locale.ROOT);
        if (!MANUAL_CODE_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Manual employee ID must match pattern: A-Z0-9 plus _ or - (3-32 chars)");
        }
        if (userRepository.existsByCode(normalized)) {
            throw new ResponseStatusException(BAD_REQUEST, "Employee ID already exists. Please choose a different one.");
        }
        log.info(
                "AUDIT_EMPLOYEE_CODE_OVERRIDE actorId={} actorRole={} organizationId={} manualCode={}",
                actor != null ? actor.getId() : null,
                actor != null ? actor.getRole() : null,
                org.getId(),
                normalized
        );
        return normalized;
    }

    private String generateAutomaticCode(Organization org) {
        String prefix = sanitizePrefix(org.getEmployeeCodePrefix());
        int padding = sanitizePadding(org.getEmployeeCodePadding());
        int next = sanitizeNextNumber(org.getNextEmployeeCodeNumber());

        int candidateNumber = next;
        String candidate = toCode(prefix, padding, candidateNumber);
        while (userRepository.existsByCode(candidate)) {
            candidateNumber++;
            candidate = toCode(prefix, padding, candidateNumber);
        }
        org.setEmployeeCodePrefix(prefix);
        org.setEmployeeCodePadding(padding);
        org.setNextEmployeeCodeNumber(candidateNumber + 1);
        organizationRepository.save(org);
        return candidate;
    }

    private String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "EMP";
        return prefix.trim().toUpperCase(Locale.ROOT);
    }

    private int sanitizePadding(Integer padding) {
        if (padding == null || padding < 1) return 4;
        return Math.min(padding, 8);
    }

    private int sanitizeNextNumber(Integer next) {
        if (next == null || next < 1) return 1;
        return next;
    }

    private String toCode(String prefix, int padding, int number) {
        return prefix + String.format("%0" + padding + "d", number);
    }
}

