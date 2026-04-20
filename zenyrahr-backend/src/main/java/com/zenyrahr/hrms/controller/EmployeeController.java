package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Timesheet.ProjectService;
import com.zenyrahr.hrms.Timesheet.Project;
import com.zenyrahr.hrms.dto.ProjectResponseDTO;
import com.zenyrahr.hrms.dto.AttendanceDTO;
import com.zenyrahr.hrms.dto.EmployeeJobChangeRequestDTO;
import com.zenyrahr.hrms.dto.EmployeeJobHistoryDTO;
import com.zenyrahr.hrms.model.Documents;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.AttendanceService;
import com.zenyrahr.hrms.service.DocumentsService;
import com.zenyrahr.hrms.service.EmployeeJobChangeService;
import com.zenyrahr.hrms.service.EmployeeCodeService;
import com.zenyrahr.hrms.service.EmployeeLeaveBalanceService;
import com.zenyrahr.hrms.service.LocalFileStorageService;
import com.zenyrahr.hrms.service.OrganizationRoleService;
import com.zenyrahr.hrms.service.OrganizationService;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/employees")
@Slf4j
public class EmployeeController {

    private final UserDetailsService employeeService;
    private final EmployeeLeaveBalanceService employeeLeaveBalanceService;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final EmployeeCodeService employeeCodeService;
    private final OrganizationRoleService organizationRoleService;
    private final OrganizationService organizationService;
    private final AttendanceService attendanceService;
    private final DocumentsService documentsService;
    private final LocalFileStorageService localFileStorageService;
    private final ProjectService projectService;
    private final EmployeeJobChangeService employeeJobChangeService;
    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name}")
    private String brevoSenderName;

//
//    @GetMapping
//    public ResponseEntity<List<Employee>> getAllEmployee() {
//        List<Employee> users = employeeService.getAllEmployee();
//        return ResponseEntity.ok(users);
//    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertOrganizationActive(actor);
            tenantAccessService.assertCanManageEmployees(actor);
            tenantAccessService.assertOrgAdminDoesNotCreatePrivilegedUsers(actor, employee);
            Long scopedOrgId = tenantAccessService.scopedOrganizationForNewEmployee(actor, employee)
                    .orElseThrow(() -> new IllegalArgumentException("Organization is required"));
            employee.setOrganization(organizationRepository.findById(scopedOrgId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found")));
            String requestedRole = organizationRoleService.normalizeAssignableRole(employee.getRole());
            organizationRoleService.assertRoleExistsForOrganization(scopedOrgId, requestedRole);
            organizationService.assertCanAddActiveUsers(scopedOrgId, 1);
            employee.setRole(requestedRole);
            employee.setCode(employeeCodeService.resolveCodeForNewEmployee(actor, employee.getOrganization(), employee.getCode()));

            String rawPassword = employee.getPassword();
            boolean generatedPassword = false;
            if (rawPassword == null || rawPassword.isBlank()) {
                rawPassword = generateRandomPassword();
                employee.setPassword(rawPassword);
                employee.setFirstLogin(true);
                generatedPassword = true;
            }

            Employee createdEmployee = employeeService.saveEmployee(employee);
            employeeLeaveBalanceService.initializeLeaveBalancesForEmployee(createdEmployee);

            if (generatedPassword) {
                sendWelcomeEmail(
                        createdEmployee.getFirstName() + " " + createdEmployee.getLastName(),
                        createdEmployee.getUsername(),
                        rawPassword
                );
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            log.error("Employee created but failed to send welcome email", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Employee created, but failed to send welcome email.");
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private String buildEmailContent(String employeeName, String username, String password) {
        return "Dear " + employeeName + ",\n\n"
                + "Your account has been created in ZenyraHR.\n\n"
                + "Username: " + username + "\n"
                + "Password: " + password + "\n\n"
                + "Please log in and change your password at first login.\n\n"
                + "Regards,\nHR Team";
    }

    private void sendWelcomeEmail(String employeeName, String username, String password) throws Exception {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);

        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().email(brevoSenderEmail).name(brevoSenderName));
        sendSmtpEmail.setTo(List.of(new SendSmtpEmailTo().email(username).name(employeeName)));
        sendSmtpEmail.setSubject("Welcome to ZenyraHR");
        sendSmtpEmail.setHtmlContent("<html><body>"
                + buildEmailContent(employeeName, username, password).replace("\n", "<br>")
                + "</body></html>");

        apiInstance.sendTransacEmail(sendSmtpEmail);
    }

    @GetMapping("/{id}")
    public Optional<Employee> getEmployee(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Main admin cannot view organization employees");
        }
        Employee target = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        tenantAccessService.assertCanAccessEmployee(actor, target);
        applyEmergencyContactPrivacy(actor, target);
        if (tenantAccessService.isManager(actor)) {
            attachTodayAttendance(target);
        }
        return Optional.of(target);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertCanManageEmployees(actor);
            Employee target = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            tenantAccessService.assertCanAccessEmployee(actor, target);
            employeeService.deleteEmployee(id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete employee because related records exist (leave, expense, travel, tickets, or payroll). Remove dependencies or deactivate employee.");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee updatedEmployee) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Employee target = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        tenantAccessService.assertCanAccessEmployee(actor, target);

        boolean selfUpdate = Objects.equals(actor.getId(), target.getId());
        if (!selfUpdate) {
            tenantAccessService.assertCanManageEmployees(actor);
        } else if (!tenantAccessService.canManageEmployees(actor)) {
            // Self-service profile update: client cannot change access-controlled fields.
            updatedEmployee.setRole(target.getRole());
            updatedEmployee.setActive(Boolean.TRUE.equals(target.getActive()));
            updatedEmployee.setOrganization(target.getOrganization());
        }

        tenantAccessService.assertHrCannotModifyPrivilegedUser(actor, target);
        String nextRole = updatedEmployee.getRole() != null && !updatedEmployee.getRole().isBlank()
                ? organizationRoleService.normalizeAssignableRole(updatedEmployee.getRole())
                : (target.getRole() == null ? "" : target.getRole().toLowerCase());
        updatedEmployee.setRole(nextRole);
        tenantAccessService.assertOrgAdminDoesNotCreatePrivilegedUsers(actor, updatedEmployee, target);
        if (!tenantAccessService.isMainAdmin(actor)) {
            updatedEmployee.setOrganization(target.getOrganization());
        }
        Long scopedOrgId = updatedEmployee.getOrganization() != null ? updatedEmployee.getOrganization().getId() : null;
        if (scopedOrgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization is required");
        }
        organizationRoleService.assertRoleExistsForOrganization(scopedOrgId, nextRole);
        boolean targetWasActive = Boolean.TRUE.equals(target.getActive());
        boolean requestedActive = Boolean.TRUE.equals(updatedEmployee.getActive());
        boolean isMovingActiveUser =
                requestedActive
                        && target.getOrganization() != null
                        && !scopedOrgId.equals(target.getOrganization().getId());
        boolean isReactivatingByUpdate = !targetWasActive && requestedActive;
        if (isMovingActiveUser || isReactivatingByUpdate) {
            organizationService.assertCanAddActiveUsers(scopedOrgId, 1);
        }
        Employee updated = employeeService.updateEmployee(id, updatedEmployee);

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Profile image file is required");
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Profile image size exceeds maximum limit of 2MB");
            }

            Employee actor = tenantAccessService.requireCurrentEmployee();
            Employee target = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            tenantAccessService.assertCanAccessEmployee(actor, target);

            String imagePath = localFileStorageService.storeProfilePicture(file, id.toString());
            Documents documents = Optional.ofNullable(target.getDocuments()).orElseGet(() -> {
                Documents newDocuments = new Documents();
                newDocuments.setEmployee(target);
                target.setDocuments(newDocuments);
                return newDocuments;
            });
            documents.setProfileImageUrl(imagePath);
            documentsService.saveDocuments(documents);

            return ResponseEntity.ok(new UploadResponse("Profile picture uploaded successfully", imagePath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile image");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{employeeId}/assign-manager/{managerId}")
    public Employee assignManager(@PathVariable Long employeeId, @PathVariable Long managerId)
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanManageEmployees(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        tenantAccessService.assertCanAccessEmployeeId(actor, managerId);
        return employeeService.assignReportingManager(employeeId, managerId);
    }

    @PostMapping("/{employeeId}/assign-projects")
    public Employee assignProjects(@PathVariable Long employeeId, @RequestBody List<Long> projectIds)
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanManageEmployees(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return employeeService.assignProjectsToEmployee(employeeId, projectIds);
    }

    @GetMapping("/{employeeId}/projects")
    public List<ProjectResponseDTO> getAssignedProjects(@PathVariable Long employeeId)
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return projectService.getAssignedProjectResponses(actor, employeeId);
    }

    @PutMapping("/{employeeId}/manager/{managerId}")
    public ResponseEntity<Employee> updateEmployeeManager(
            @PathVariable Long employeeId,
            @PathVariable Long managerId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanManageEmployees(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        tenantAccessService.assertCanAccessEmployeeId(actor, managerId);

        Employee updatedEmployee = employeeService.assignReportingManager(employeeId, managerId);
        return ResponseEntity.ok(updatedEmployee);
    }

    // New endpoint for removing manager
    @DeleteMapping("/{employeeId}/manager")
    public Employee removeManager(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanManageEmployees(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return employeeService.removeReportingManager(employeeId);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateEmployee(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertCanManageEmployees(actor);
            Employee target = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            tenantAccessService.assertCanAccessEmployee(actor, target);
            tenantAccessService.assertHrCannotModifyPrivilegedUser(actor, target);
            Employee employee = employeeService.deactivateEmployee(id);
            return ResponseEntity.ok(employee);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateEmployee(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertCanManageEmployees(actor);
            Employee target = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            tenantAccessService.assertCanAccessEmployee(actor, target);
            tenantAccessService.assertHrCannotModifyPrivilegedUser(actor, target);
            if (!Boolean.TRUE.equals(target.getActive())
                    && target.getOrganization() != null
                    && target.getOrganization().getId() != null) {
                organizationService.assertCanAddActiveUsers(target.getOrganization().getId(), 1);
            }
            Employee employee = employeeService.reactivateEmployee(id);
            return ResponseEntity.ok(employee);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @PostMapping("/{id}/job-change")
    public ResponseEntity<EmployeeJobHistoryDTO> applyEmployeeJobChange(
            @PathVariable Long id,
            @RequestBody EmployeeJobChangeRequestDTO request
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        EmployeeJobHistoryDTO response = employeeJobChangeService.applyJobChange(actor, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/job-history")
    public ResponseEntity<List<EmployeeJobHistoryDTO>> getEmployeeJobHistory(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<EmployeeJobHistoryDTO> history = employeeJobChangeService.getEmployeeJobHistory(actor, id);
        return ResponseEntity.ok(history);
    }
    // Additional endpoints for updating and CRUD operations on related entities


//    @PostMapping
//    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
//        Employee employee = employeeService.createEmployee(employeeDTO);
//        return new ResponseEntity<>(employee, HttpStatus.CREATED);
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<Employee> getEmployee(@PathVariable UUID id) {
//        Employee employee = employeeService.getEmployee(id);
//        return new ResponseEntity<>(employee, HttpStatus.OK);
//    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Main admin cannot view organization employees");
        }
        List<Employee> employees;
        if (tenantAccessService.isManager(actor)) {
            employees = tenantAccessService.listDirectReportsForManager(actor);
            employees.forEach(this::attachTodayAttendance);
        } else {
            employees = employeeService.getAllEmployee();
            employees = tenantAccessService.filterEmployeesByScope(actor, employees);
        }
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }

    private void attachTodayAttendance(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return;
        }
        AttendanceDTO today = attendanceService.getTodayAttendance(employee.getId());
        if (today == null) {
            employee.setTodayCheckInTime(null);
            employee.setTodayCheckOutTime(null);
            return;
        }
        employee.setTodayCheckInTime(today.getCheckInTime());
        employee.setTodayCheckOutTime(today.getCheckOutTime());
    }

    private void applyEmergencyContactPrivacy(Employee actor, Employee target) {
        if (actor == null || target == null || actor.getId() == null || target.getId() == null) {
            return;
        }

        boolean isSelfView = Objects.equals(actor.getId(), target.getId());
        String actorRole = actor.getRole() == null ? "" : actor.getRole().toLowerCase();
        boolean isHrOrOrgAdmin = "hr".equals(actorRole) || "org_admin".equals(actorRole);
        boolean hasConsent = Boolean.TRUE.equals(target.getAllowEmergencyContactVisibilityToHr());

        // Enforce privacy at API layer so sensitive data cannot leak via UI bypass.
        if (!isSelfView && isHrOrOrgAdmin && !hasConsent) {
            target.setEmergencyContactName(null);
            target.setEmergencyContactRelation(null);
            target.setEmergencyContactNumber(null);
            target.setAlternateContactNumber(null);
            target.setFamilyDetails(null);
        }
    }

    private record UploadResponse(String message, String fileUrl) {}

//
//    @PutMapping("/{id}")
//    public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody EmployeeDTO employeeDTO) {
//        Employee employee = employeeService.updateEmployee(id, employeeDTO);
//        return new ResponseEntity<>(employee, HttpStatus.OK);
//    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
//        employeeService.deleteEmployee(id);
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }
}
