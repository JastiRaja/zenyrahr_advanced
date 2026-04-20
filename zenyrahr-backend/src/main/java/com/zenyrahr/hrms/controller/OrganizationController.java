package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.Timesheet.ProjectRepository;
import com.zenyrahr.hrms.dto.AssignEmployeesRequest;
import com.zenyrahr.hrms.dto.EmployeeCodeSettingsDTO;
import com.zenyrahr.hrms.dto.OrganizationOverviewDTO;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.EmployeeCodeService;
import com.zenyrahr.hrms.service.LocalFileStorageService;
import com.zenyrahr.hrms.service.OrganizationService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final TenantAccessService tenantAccessService;
    private final EmployeeCodeService employeeCodeService;
    private final LocalFileStorageService localFileStorageService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private Employee requireMainAdmin() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (!tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only main admin can manage organizations");
        }
        return actor;
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/overview")
    public ResponseEntity<List<OrganizationOverviewDTO>> getOrganizationOverview() {
        requireMainAdmin();
        List<OrganizationOverviewDTO> overview = organizationService.getAllOrganizations().stream()
                .map(org -> new OrganizationOverviewDTO(
                        org.getId(),
                        org.getCode(),
                        org.getName(),
                        org.getAddress(),
                        org.getLogoUrl(),
                        org.getActive(),
                        userRepository.countByOrganization_Id(org.getId()),
                        userRepository.countByOrganization_IdAndActiveTrue(org.getId()),
                        Boolean.FALSE.equals(org.getTimesheetEnabled())
                                ? 0L
                                : projectRepository.countByOrganization_IdAndStatusIgnoreCase(org.getId(), "ACTIVE"),
                        org.getMaxActiveUsers(),
                        org.getTimesheetEnabled(),
                        org.getEmployeeManagementEnabled(),
                        org.getSelfServiceEnabled(),
                        org.getAttendanceEnabled(),
                        org.getLeaveManagementEnabled(),
                        org.getHolidayManagementEnabled(),
                        org.getPayrollEnabled(),
                        org.getTravelEnabled(),
                        org.getExpenseEnabled()
                ))
                .toList();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/current/menu-settings")
    public ResponseEntity<Map<String, Boolean>> getCurrentOrganizationMenuSettings() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor) || actor.getOrganization() == null) {
            return ResponseEntity.ok(Map.of(
                    "timesheetEnabled", true,
                    "employeeManagementEnabled", true,
                    "selfServiceEnabled", true,
                    "attendanceEnabled", true,
                    "leaveManagementEnabled", true,
                    "holidayManagementEnabled", true,
                    "payrollEnabled", true,
                    "travelEnabled", true,
                    "expenseEnabled", true
            ));
        }
        Organization org = organizationService.getOrganizationById(actor.getOrganization().getId());
        return ResponseEntity.ok(Map.of(
                "timesheetEnabled", !Objects.equals(org.getTimesheetEnabled(), false),
                "employeeManagementEnabled", !Objects.equals(org.getEmployeeManagementEnabled(), false),
                "selfServiceEnabled", !Objects.equals(org.getSelfServiceEnabled(), false),
                "attendanceEnabled", !Objects.equals(org.getAttendanceEnabled(), false),
                "leaveManagementEnabled", !Objects.equals(org.getLeaveManagementEnabled(), false),
                "holidayManagementEnabled", !Objects.equals(org.getHolidayManagementEnabled(), false),
                "payrollEnabled", !Objects.equals(org.getPayrollEnabled(), false),
                "travelEnabled", !Objects.equals(org.getTravelEnabled(), false),
                "expenseEnabled", !Objects.equals(org.getExpenseEnabled(), false)
        ));
    }

    @GetMapping("/current/branding")
    public ResponseEntity<Map<String, String>> getCurrentOrganizationBranding() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor) || actor.getOrganization() == null) {
            return ResponseEntity.ok(Map.of(
                    "organizationName", "",
                    "logoUrl", ""
            ));
        }

        Organization org = organizationService.getOrganizationById(actor.getOrganization().getId());
        return ResponseEntity.ok(Map.of(
                "organizationName", org.getName() == null ? "" : org.getName(),
                "logoUrl", org.getLogoUrl() == null ? "" : org.getLogoUrl()
        ));
    }

    @GetMapping("/current/employee-code-settings")
    public ResponseEntity<EmployeeCodeSettingsDTO> getCurrentEmployeeCodeSettings() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor) || actor.getOrganization() == null) {
            EmployeeCodeSettingsDTO dto = new EmployeeCodeSettingsDTO();
            dto.setEmployeeCodePrefix("EMP");
            dto.setEmployeeCodePadding(4);
            dto.setNextEmployeeCodeNumber(1);
            dto.setAllowManualEmployeeCodeOverride(false);
            return ResponseEntity.ok(dto);
        }
        Organization org = organizationService.getOrganizationById(actor.getOrganization().getId());
        return ResponseEntity.ok(toEmployeeCodeSettings(org));
    }

    @PutMapping("/current/employee-code-settings")
    public ResponseEntity<EmployeeCodeSettingsDTO> updateCurrentEmployeeCodeSettings(
            @RequestBody EmployeeCodeSettingsDTO payload
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (actor.getOrganization() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Organization assignment is required");
        }
        Organization org = organizationService.getOrganizationById(actor.getOrganization().getId());
        applyEmployeeCodeSettings(actor, org, payload);
        Organization updated = organizationService.updateOrganization(org.getId(), org);
        return ResponseEntity.ok(toEmployeeCodeSettings(updated));
    }

    @GetMapping("/{id}/employee-code-settings")
    public ResponseEntity<EmployeeCodeSettingsDTO> getEmployeeCodeSettingsByOrganization(@PathVariable Long id) {
        requireMainAdmin();
        Organization org = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(toEmployeeCodeSettings(org));
    }

    @PutMapping("/{id}/employee-code-settings")
    public ResponseEntity<EmployeeCodeSettingsDTO> updateEmployeeCodeSettingsByOrganization(
            @PathVariable Long id,
            @RequestBody EmployeeCodeSettingsDTO payload
    ) {
        Employee actor = requireMainAdmin();
        Organization org = organizationService.getOrganizationById(id);
        applyEmployeeCodeSettings(actor, org, payload);
        Organization updated = organizationService.updateOrganization(id, org);
        return ResponseEntity.ok(toEmployeeCodeSettings(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable Long id) {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Organization organization) {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.createOrganization(organization));
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<Map<String, String>> uploadOrganizationLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "organizationName", required = false) String organizationName
    ) throws Exception {
        requireMainAdmin();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        String relativePath = localFileStorageService.storeOrganizationLogo(file, organizationName);
        String logoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativePath)
                .toUriString();
        return ResponseEntity.ok(Map.of("logoUrl", logoUrl));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable Long id,
            @RequestBody Organization organization
    ) {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.updateOrganization(id, organization));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        requireMainAdmin();
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Organization> disableOrganization(@PathVariable Long id) {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.setOrganizationActive(id, false));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<Organization> enableOrganization(@PathVariable Long id) {
        requireMainAdmin();
        return ResponseEntity.ok(organizationService.setOrganizationActive(id, true));
    }

    @PostMapping("/{id}/assign-employees")
    public ResponseEntity<Map<String, Object>> assignEmployees(
            @PathVariable Long id,
            @RequestBody AssignEmployeesRequest request
    ) {
        requireMainAdmin();
        int count = organizationService.assignEmployeesToOrganization(id, request.getEmployeeIds());
        return ResponseEntity.ok(Map.of(
                "assignedCount", count,
                "organizationId", id
        ));
    }

    private EmployeeCodeSettingsDTO toEmployeeCodeSettings(Organization org) {
        EmployeeCodeSettingsDTO dto = new EmployeeCodeSettingsDTO();
        dto.setEmployeeCodePrefix(org.getEmployeeCodePrefix());
        dto.setEmployeeCodePadding(org.getEmployeeCodePadding());
        dto.setNextEmployeeCodeNumber(org.getNextEmployeeCodeNumber());
        dto.setAllowManualEmployeeCodeOverride(org.getAllowManualEmployeeCodeOverride());
        return dto;
    }

    private void applyEmployeeCodeSettings(Employee actor, Organization org, EmployeeCodeSettingsDTO payload) {
        employeeCodeService.validateSettingsForUpdate(
                actor,
                org,
                payload.getEmployeeCodePrefix(),
                payload.getEmployeeCodePadding(),
                payload.getNextEmployeeCodeNumber(),
                payload.getAllowManualEmployeeCodeOverride()
        );
        org.setEmployeeCodePrefix(payload.getEmployeeCodePrefix().trim().toUpperCase());
        org.setEmployeeCodePadding(payload.getEmployeeCodePadding());
        org.setNextEmployeeCodeNumber(payload.getNextEmployeeCodeNumber());
        org.setAllowManualEmployeeCodeOverride(payload.getAllowManualEmployeeCodeOverride());
    }
}
