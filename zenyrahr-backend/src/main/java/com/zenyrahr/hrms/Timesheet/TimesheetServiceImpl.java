package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.ApprovalModule;
import com.zenyrahr.hrms.model.Employee;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zenyrahr.hrms.service.EmployeeTableService;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@RequiredArgsConstructor
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final UserRepository userRepository;
    private final SequenceService sequenceService;
    private final TenantAccessService tenantAccessService;
    private final ApprovalHierarchyService approvalHierarchyService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmployeeTableService employeeTableService;

    public Timesheet createTimesheetcreate(Employee employee, LocalDateTime startTime, LocalDateTime endTime, String taskDescription, String comments) {
        Timesheet timesheet = new Timesheet();
        Project project = projectRepository.findById(timesheet.getProject().getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        timesheet.setProject(project);
        timesheet.setEmployeeId(employee.getId());
        timesheet.setHoursWorked(timesheet.getHoursWorked());
        timesheet.setDate(timesheet.getDate());
        timesheet.setDescription(taskDescription);
        timesheet.setStatus(timesheet.getStatus());

        // Proceed to save the Timesheet
        return timesheetRepository.save(timesheet);
    }

    @Override
    public Timesheet createTimesheet(Timesheet timesheet) {
        Project project = projectRepository.findById(timesheet.getProject().getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        timesheet.setProject(project);
        timesheet.setCode(sequenceService.getNextCode("TM"));
        Employee targetEmployee = userRepository.findById(timesheet.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        int requiredLevels = approvalHierarchyService.getRequiredApprovalLevels(targetEmployee, ApprovalModule.TIMESHEET);
        timesheet.setStatus(TimeSheetStatus.PENDING);
        timesheet.setCurrentApprovalLevel(1);
        timesheet.setMaxApprovalLevel(requiredLevels);

        // Proceed to save the Timesheet
        return timesheetRepository.save(timesheet);
    }
    @Override
    public Optional<Timesheet> getTimesheetById(Long id) {
        return timesheetRepository.findById(id);
    }

    @Override
    public List<Timesheet> getAllTimesheets(Employee actor) {
        List<Timesheet> allTimesheets = timesheetRepository.findAll();
        if (tenantAccessService.isMainAdmin(actor)) {
            // Platform admin is not part of any organization's timesheet workflow; avoid
            // cross-tenant listings (approvals, notifications, dashboards) pulling all rows.
            return List.of();
        }

        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        if (tenantAccessService.canManageEmployees(actor)) {
            return allTimesheets.stream()
                    .filter(timesheet -> belongsToOrganization(timesheet, actorOrgId))
                    .map(timesheet -> enrichApprovalContext(actor, timesheet))
                    .toList();
        }

        if (tenantAccessService.isManager(actor)) {
            Set<Long> directReportIds = tenantAccessService.listDirectReportsForManager(actor).stream()
                    .map(Employee::getId)
                    .collect(Collectors.toSet());
            return allTimesheets.stream()
                    .filter(timesheet -> directReportIds.contains(timesheet.getEmployeeId()))
                    .map(timesheet -> enrichApprovalContext(actor, timesheet))
                    .toList();
        }

        return allTimesheets.stream()
                .filter(timesheet -> Objects.equals(timesheet.getEmployeeId(), actor.getId()))
                .map(timesheet -> enrichApprovalContext(actor, timesheet))
                .toList();
    }

    public Optional<Timesheet> getAllTimesheets(Long id, Long employeeId) {
        return timesheetRepository.findByIdAndEmployeeId(id, employeeId);
    }

//    @Override
//    public Timesheet approveTimesheet(Long id) {
//        Optional<Timesheet> timesheet = timesheetRepository.findById(id);
//        if (timesheet.isPresent()) {
//            Timesheet t = timesheet.get();
//            t.setStatus(TimeSheetStatus.APPROVED);
//            return timesheetRepository.save(t);
//        }
//        return null;
//    }

    @Override
    public List<Timesheet> getTimesheetsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findTimesheetsBetweenDates(startDate, endDate);
    }
    @Override
    public void deleteTimesheet(Long id) {
        timesheetRepository.deleteById(id);
    }

    @Override
    public List<Timesheet> getTimesheetsByEmployee(Employee actor, Long employeeId) {
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return timesheetRepository.findByEmployeeId(employeeId).stream()
                .map(timesheet -> enrichApprovalContext(actor, timesheet))
                .toList();
    }

    @Override
    public Timesheet approveTimesheet(Long id, Long approverId, String requiredComments, int requiredLevels) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found with id: " + id));
        if (timesheet.getStatus() != TimeSheetStatus.PENDING) {
            throw new RuntimeException("Timesheet is not pending approval.");
        }
        int currentLevel = normalizeLevel(timesheet.getCurrentApprovalLevel());
        int maxLevel = Math.max(currentLevel, Math.max(1, requiredLevels));
        timesheet.setMaxApprovalLevel(maxLevel);
        timesheet.setRequiredComments(requiredComments);
        if (currentLevel >= maxLevel) {
            timesheet.setStatus(TimeSheetStatus.APPROVED);
        } else {
            timesheet.setStatus(TimeSheetStatus.PENDING);
            timesheet.setCurrentApprovalLevel(currentLevel + 1);
        }
        return timesheetRepository.save(timesheet);
    }

    @Override
    public Timesheet rejectTimesheet(Long id, Long employeeId, String requiredComments) {
        // Find the timesheet entry by ID
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found with id: " + id));
        if (timesheet.getStatus() != TimeSheetStatus.PENDING) {
            throw new RuntimeException("Timesheet is not pending approval.");
        }

        // Update status to REJECTED
        timesheet.setStatus(TimeSheetStatus.REJECTED);
        timesheet.setRequiredComments(requiredComments);

        // Save and return the updated timesheet
        return timesheetRepository.save(timesheet);
    }


    @Override
    public Timesheet updateTimesheet(Long id, Timesheet updatedTimesheet) {
        return timesheetRepository.findById(id).map(existingTimesheet -> {
            existingTimesheet.setDate(updatedTimesheet.getDate());
            existingTimesheet.setHoursWorked(updatedTimesheet.getHoursWorked());
            existingTimesheet.setDescription(updatedTimesheet.getDescription());
            existingTimesheet.setRequiredComments(updatedTimesheet.getRequiredComments());
            existingTimesheet.setStatus(updatedTimesheet.getStatus());
            existingTimesheet.setProject(updatedTimesheet.getProject());
            return timesheetRepository.save(existingTimesheet);
        }).orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + id));
    }

    @Override
    public Timesheet withdrawTimesheet(Long id) {
        return timesheetRepository.findById(id).map(existingTimesheet -> {
            if (!existingTimesheet.getStatus().equals(TimeSheetStatus.PENDING)) {
                throw new RuntimeException("Timesheet cannot be withdrawn. Only pending timesheets can be withdrawn.");
            }
            existingTimesheet.setStatus(TimeSheetStatus.WITHDRAWN);
            return timesheetRepository.save(existingTimesheet);
        }).orElseThrow(() -> new RuntimeException("Timesheet not found with id: " + id));
    }

    @Override
    public List<Timesheet> getTimesheetsByStatus(TimeSheetStatus status) {
        return timesheetRepository.findByStatus(status);
    }

    @Override
    public List<Timesheet> getTimesheetsByStatusAndEmployeeId(TimeSheetStatus status, Long employeeId) {
        return timesheetRepository.findByStatusAndEmployeeId(status, employeeId);
    }

    private boolean belongsToOrganization(Timesheet timesheet, Long organizationId) {
        Employee employee = userRepository.findById(timesheet.getEmployeeId()).orElse(null);
        Long employeeOrgId = employee != null && employee.getOrganization() != null
                ? employee.getOrganization().getId()
                : null;
        return Objects.equals(employeeOrgId, organizationId);
    }

    private Timesheet enrichApprovalContext(Employee actor, Timesheet timesheet) {
        int currentLevel = normalizeLevel(timesheet.getCurrentApprovalLevel());
        int maxLevel = normalizeLevel(timesheet.getMaxApprovalLevel());
        if (maxLevel < currentLevel) {
            maxLevel = currentLevel;
        }
        timesheet.setCurrentApprovalLevel(currentLevel);
        timesheet.setMaxApprovalLevel(maxLevel);
        boolean canApprove = false;
        if (timesheet.getStatus() == TimeSheetStatus.PENDING) {
            Employee target = userRepository.findById(timesheet.getEmployeeId()).orElse(null);
            if (target != null) {
                try {
                    approvalHierarchyService.assertActorCanApprove(actor, target, ApprovalModule.TIMESHEET, currentLevel);
                    canApprove = true;
                } catch (ResponseStatusException ex) {
                    canApprove = false;
                }
            }
        }
        timesheet.setCanCurrentUserApprove(canApprove);
        return timesheet;
    }

    private int normalizeLevel(Integer rawLevel) {
        return rawLevel == null || rawLevel <= 0 ? 1 : rawLevel;
    }

}
