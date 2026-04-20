package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.ApprovalModule;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/timesheet")
@RequiredArgsConstructor
@Slf4j
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final TenantAccessService tenantAccessService;
    private final ApprovalHierarchyService approvalHierarchyService;
    private final UserRepository userRepository;

    @PutMapping("/{id}")
    public ResponseEntity<Timesheet> updateTimesheet(@PathVariable Long id, @RequestBody Timesheet updatedTimesheet) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanModifyTimesheet(actor, updatedTimesheet.getEmployeeId());
        Timesheet updatedEntry = timesheetService.updateTimesheet(id, updatedTimesheet);
        return ResponseEntity.ok(updatedEntry);
    }

    @PutMapping("/withdraw/{id}")
    public ResponseEntity<Timesheet> withdrawTimesheet(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Timesheet current = timesheetService.getTimesheetById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Timesheet not found"));
        assertCanModifyTimesheet(actor, current.getEmployeeId());
        Timesheet withdrawnEntry = timesheetService.withdrawTimesheet(id);
        return ResponseEntity.ok(withdrawnEntry);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Timesheet>> getTimesheetsByEmployee(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        List<Timesheet> timesheets = timesheetService.getTimesheetsByEmployee(actor, employeeId);
        return ResponseEntity.ok(timesheets);
    }

    @GetMapping
    public ResponseEntity<List<Timesheet>> getAllTimesheets() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(timesheetService.getAllTimesheets(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Timesheet> getTimesheetById(@PathVariable Long id) {
        return timesheetService.getTimesheetById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Timesheet> createTimesheet(@RequestBody Timesheet timesheet) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanModifyTimesheet(actor, timesheet.getEmployeeId());
        Timesheet createdTimesheet = timesheetService.createTimesheet(timesheet);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTimesheet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimesheet(@PathVariable Long id) {
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/approve/{id}")
    public Timesheet approveTimesheet(
            @PathVariable("id") Long id,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("approved") boolean approved,
            @RequestParam("requiredComments") String requiredComments
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertActorMatchesApprover(actor, employeeId);
        Timesheet current = timesheetService.getTimesheetById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Timesheet not found"));
        if (!approved) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheet approval request must set approved=true.");
        }
        int currentLevel = current.getCurrentApprovalLevel() == null || current.getCurrentApprovalLevel() <= 0
                ? 1
                : current.getCurrentApprovalLevel();
        Employee targetEmployee = userRepository.findById(current.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Employee not found"));
        int requiredLevels = approvalHierarchyService.getRequiredApprovalLevels(targetEmployee, ApprovalModule.TIMESHEET);
        approvalHierarchyService.assertActorCanApprove(actor, targetEmployee, ApprovalModule.TIMESHEET, currentLevel);
        log.debug("Approving timesheet id={}, approverId={}, level={}/{}", id, actor.getId(), currentLevel, requiredLevels);
        return timesheetService.approveTimesheet(id, actor.getId(), requiredComments, requiredLevels);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<Timesheet> rejectTimesheet(
            @PathVariable Long id,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("requiredComments") String requiredComments) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertActorMatchesApprover(actor, employeeId);
        Timesheet current = timesheetService.getTimesheetById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Timesheet not found"));
        int currentLevel = current.getCurrentApprovalLevel() == null || current.getCurrentApprovalLevel() <= 0
                ? 1
                : current.getCurrentApprovalLevel();
        Employee targetEmployee = userRepository.findById(current.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Employee not found"));
        approvalHierarchyService.assertActorCanApprove(actor, targetEmployee, ApprovalModule.TIMESHEET, currentLevel);
        Timesheet rejectedTimesheet = timesheetService.rejectTimesheet(id, actor.getId(), requiredComments);
        return ResponseEntity.ok(rejectedTimesheet);
    }

    @GetMapping("/between-dates")
    public ResponseEntity<List<Timesheet>> getTimesheetsBetweenDates(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate
    ) {
        List<Timesheet> timesheets = timesheetService.getTimesheetsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(timesheets);
    }

    @GetMapping("/pending")
    public List<Timesheet> getPendingTimesheets(@RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (employeeId != null) {
            return timesheetService.getTimesheetsByEmployee(actor, employeeId).stream()
                    .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.PENDING)
                    .toList();
        }
        return timesheetService.getAllTimesheets(actor).stream()
                .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.PENDING)
                .toList();
    }

    @GetMapping("/approved")
    public List<Timesheet> getApprovedTimesheets(@RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (employeeId != null) {
            return timesheetService.getTimesheetsByEmployee(actor, employeeId).stream()
                    .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.APPROVED)
                    .toList();
        }
        return timesheetService.getAllTimesheets(actor).stream()
                .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.APPROVED)
                .toList();
    }

    private void assertCanModifyTimesheet(Employee actor, Long targetEmployeeId) {
        if (targetEmployeeId == null) {
            throw new ResponseStatusException(FORBIDDEN, "Employee is required for timesheet changes");
        }
        if (tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        if (!Objects.equals(actor.getId(), targetEmployeeId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only modify your own timesheet");
        }
    }

    private void assertActorMatchesApprover(Employee actor, Long approverId) {
        if (approverId == null || tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        if (!Objects.equals(actor.getId(), approverId)) {
            throw new ResponseStatusException(FORBIDDEN, "Approver ID must match authenticated user");
        }
    }
}
