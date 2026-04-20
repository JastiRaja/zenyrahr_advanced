package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.EmployeeJobHistoryRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.dto.EmployeeJobChangeRequestDTO;
import com.zenyrahr.hrms.dto.EmployeeJobHistoryDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeJobHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class EmployeeJobChangeService {

    private static final String CHANGE_PROMOTION = "PROMOTION";
    private static final String CHANGE_ROLE = "ROLE_CHANGE";
    private static final String CHANGE_TRANSFER = "TRANSFER";
    private static final String CHANGE_JOB_UPDATE = "JOB_UPDATE";

    private final UserRepository userRepository;
    private final EmployeeJobHistoryRepository employeeJobHistoryRepository;
    private final TenantAccessService tenantAccessService;
    private final OrganizationRoleService organizationRoleService;

    @Transactional
    public EmployeeJobHistoryDTO applyJobChange(Employee actor, Long employeeId, EmployeeJobChangeRequestDTO request) {
        tenantAccessService.assertCanManageEmployees(actor);

        Employee target = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Employee not found"));
        tenantAccessService.assertCanAccessEmployee(actor, target);
        tenantAccessService.assertHrCannotModifyPrivilegedUser(actor, target);

        Long targetOrgId = target.getOrganization() != null ? target.getOrganization().getId() : null;
        if (targetOrgId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Employee must belong to an organization");
        }

        Employee managerBefore = target.getReportingManager();
        Employee managerAfter = resolveManager(target, actor, request);

        String roleBefore = target.getRole();
        String roleAfter = roleBefore;
        if (hasText(request.getRole())) {
            roleAfter = organizationRoleService.normalizeAssignableRole(request.getRole());
            organizationRoleService.assertRoleExistsForOrganization(targetOrgId, roleAfter);
            target.setRole(roleAfter);
        }

        String positionBefore = target.getPosition();
        String positionAfter = applyOptionalTextUpdate(target::setPosition, request.getPosition(), positionBefore);
        String departmentBefore = target.getDepartment();
        String departmentAfter = applyOptionalTextUpdate(target::setDepartment, request.getDepartment(), departmentBefore);
        String workLocationBefore = target.getWorkLocation();
        String workLocationAfter = applyOptionalTextUpdate(target::setWorkLocation, request.getWorkLocation(), workLocationBefore);

        if (request.isClearManager()) {
            target.setReportingManager(null);
        } else if (request.getManagerId() != null) {
            target.setReportingManager(managerAfter);
        }

        boolean changed =
                !Objects.equals(roleBefore, roleAfter)
                        || !Objects.equals(positionBefore, positionAfter)
                        || !Objects.equals(departmentBefore, departmentAfter)
                        || !Objects.equals(workLocationBefore, workLocationAfter)
                        || !Objects.equals(getId(managerBefore), getId(target.getReportingManager()));

        if (!changed) {
            throw new ResponseStatusException(BAD_REQUEST, "No job-related changes detected");
        }

        Employee savedEmployee = userRepository.save(target);

        EmployeeJobHistory history = new EmployeeJobHistory();
        history.setEmployee(savedEmployee);
        history.setChangedBy(actor);
        history.setChangeType(resolveChangeType(request.getChangeType(), roleBefore, roleAfter, departmentBefore, departmentAfter, workLocationBefore, workLocationAfter));
        history.setOldRole(roleBefore);
        history.setNewRole(roleAfter);
        history.setOldPosition(positionBefore);
        history.setNewPosition(positionAfter);
        history.setOldDepartment(departmentBefore);
        history.setNewDepartment(departmentAfter);
        history.setOldWorkLocation(workLocationBefore);
        history.setNewWorkLocation(workLocationAfter);
        history.setOldManager(managerBefore);
        history.setNewManager(savedEmployee.getReportingManager());
        history.setEffectiveDate(request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate());
        history.setReason(trimToNull(request.getReason()));

        EmployeeJobHistory savedHistory = employeeJobHistoryRepository.save(history);
        return toDto(savedHistory);
    }

    @Transactional(readOnly = true)
    public List<EmployeeJobHistoryDTO> getEmployeeJobHistory(Employee actor, Long employeeId) {
        Employee target = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Employee not found"));
        tenantAccessService.assertCanAccessEmployee(actor, target);
        return employeeJobHistoryRepository.findByEmployee_IdOrderByChangedAtDesc(employeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Employee resolveManager(Employee target, Employee actor, EmployeeJobChangeRequestDTO request) {
        if (request.getManagerId() == null || request.isClearManager()) {
            return target.getReportingManager();
        }
        if (Objects.equals(target.getId(), request.getManagerId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Employee cannot be their own manager");
        }

        Employee manager = userRepository.findById(request.getManagerId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Manager not found"));
        tenantAccessService.assertCanAccessEmployee(actor, manager);
        Long targetOrgId = target.getOrganization() != null ? target.getOrganization().getId() : null;
        Long managerOrgId = manager.getOrganization() != null ? manager.getOrganization().getId() : null;
        if (!Objects.equals(targetOrgId, managerOrgId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Manager must be from the same organization");
        }
        return manager;
    }

    private String resolveChangeType(
            String requestedChangeType,
            String oldRole,
            String newRole,
            String oldDepartment,
            String newDepartment,
            String oldWorkLocation,
            String newWorkLocation
    ) {
        if (hasText(requestedChangeType)) {
            String normalized = requestedChangeType.trim().toUpperCase(Locale.ROOT);
            if (normalized.equals(CHANGE_PROMOTION)
                    || normalized.equals(CHANGE_ROLE)
                    || normalized.equals(CHANGE_TRANSFER)
                    || normalized.equals(CHANGE_JOB_UPDATE)) {
                return normalized;
            }
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "changeType must be one of: PROMOTION, ROLE_CHANGE, TRANSFER, JOB_UPDATE"
            );
        }

        if (!Objects.equals(oldRole, newRole)) {
            return CHANGE_ROLE;
        }
        if (!Objects.equals(oldDepartment, newDepartment) || !Objects.equals(oldWorkLocation, newWorkLocation)) {
            return CHANGE_TRANSFER;
        }
        return CHANGE_JOB_UPDATE;
    }

    private EmployeeJobHistoryDTO toDto(EmployeeJobHistory history) {
        Employee employee = history.getEmployee();
        Employee changedBy = history.getChangedBy();
        Employee oldManager = history.getOldManager();
        Employee newManager = history.getNewManager();

        return EmployeeJobHistoryDTO.builder()
                .id(history.getId())
                .employeeId(getId(employee))
                .employeeName(getName(employee))
                .changedById(getId(changedBy))
                .changedByName(getName(changedBy))
                .changeType(history.getChangeType())
                .oldRole(history.getOldRole())
                .newRole(history.getNewRole())
                .oldPosition(history.getOldPosition())
                .newPosition(history.getNewPosition())
                .oldDepartment(history.getOldDepartment())
                .newDepartment(history.getNewDepartment())
                .oldWorkLocation(history.getOldWorkLocation())
                .newWorkLocation(history.getNewWorkLocation())
                .oldManagerId(getId(oldManager))
                .oldManagerName(getName(oldManager))
                .newManagerId(getId(newManager))
                .newManagerName(getName(newManager))
                .effectiveDate(history.getEffectiveDate())
                .reason(history.getReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    private String applyOptionalTextUpdate(java.util.function.Consumer<String> setter, String nextValue, String currentValue) {
        if (!hasText(nextValue)) {
            return currentValue;
        }
        String normalized = nextValue.trim();
        setter.accept(normalized);
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private Long getId(Employee employee) {
        return employee == null ? null : employee.getId();
    }

    private String getName(Employee employee) {
        if (employee == null) {
            return null;
        }
        String first = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String last = employee.getLastName() == null ? "" : employee.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? employee.getUsername() : fullName;
    }
}
