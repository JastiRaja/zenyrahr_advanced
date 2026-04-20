package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.OrganizationService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SequenceService sequenceService;

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Override
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    @Override
    public Organization createOrganization(Organization organization) {
        if (organization.getName() == null || organization.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Organization name is required");
        }
        validateMaxActiveUsers(organization.getMaxActiveUsers());
        if (organization.getCode() == null || organization.getCode().isBlank()) {
            organization.setCode(sequenceService.getNextCode("ORG"));
        }
        if (organization.getActive() == null) {
            organization.setActive(true);
        }
        if (organization.getTimesheetEnabled() == null) {
            organization.setTimesheetEnabled(true);
        }
        if (organization.getEmployeeManagementEnabled() == null) {
            organization.setEmployeeManagementEnabled(true);
        }
        if (organization.getSelfServiceEnabled() == null) {
            organization.setSelfServiceEnabled(true);
        }
        if (organization.getAttendanceEnabled() == null) {
            organization.setAttendanceEnabled(true);
        }
        if (organization.getLeaveManagementEnabled() == null) {
            organization.setLeaveManagementEnabled(true);
        }
        if (organization.getHolidayManagementEnabled() == null) {
            organization.setHolidayManagementEnabled(true);
        }
        if (organization.getPayrollEnabled() == null) {
            organization.setPayrollEnabled(true);
        }
        if (organization.getTravelEnabled() == null) {
            organization.setTravelEnabled(true);
        }
        if (organization.getExpenseEnabled() == null) {
            organization.setExpenseEnabled(true);
        }
        applyEmployeeCodeDefaults(organization);
        try {
            return organizationRepository.save(organization);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(CONFLICT, "Organization name already exists");
        }
    }

    @Override
    public Organization updateOrganization(Long id, Organization organization) {
        Organization existing = getOrganizationById(id);
        Integer requestedLimit = organization.getMaxActiveUsers() != null
                ? organization.getMaxActiveUsers()
                : existing.getMaxActiveUsers();
        validateMaxActiveUsers(requestedLimit);
        long currentActiveUsers = userRepository.countByOrganization_IdAndActiveTrue(existing.getId());
        if (requestedLimit < currentActiveUsers) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Active user limit cannot be lower than current active users (" + currentActiveUsers + ")"
            );
        }
        if (organization.getName() != null && !organization.getName().isBlank()) {
            existing.setName(organization.getName());
        }
        if (organization.getAddress() != null) {
            existing.setAddress(organization.getAddress());
        }
        if (organization.getLogoUrl() != null) {
            existing.setLogoUrl(organization.getLogoUrl());
        }
        if (organization.getTimesheetEnabled() != null) {
            existing.setTimesheetEnabled(organization.getTimesheetEnabled());
        }
        if (organization.getEmployeeManagementEnabled() != null) {
            existing.setEmployeeManagementEnabled(organization.getEmployeeManagementEnabled());
        }
        if (organization.getSelfServiceEnabled() != null) {
            existing.setSelfServiceEnabled(organization.getSelfServiceEnabled());
        }
        if (organization.getAttendanceEnabled() != null) {
            existing.setAttendanceEnabled(organization.getAttendanceEnabled());
        }
        if (organization.getLeaveManagementEnabled() != null) {
            existing.setLeaveManagementEnabled(organization.getLeaveManagementEnabled());
        }
        if (organization.getHolidayManagementEnabled() != null) {
            existing.setHolidayManagementEnabled(organization.getHolidayManagementEnabled());
        }
        if (organization.getPayrollEnabled() != null) {
            existing.setPayrollEnabled(organization.getPayrollEnabled());
        }
        if (organization.getTravelEnabled() != null) {
            existing.setTravelEnabled(organization.getTravelEnabled());
        }
        if (organization.getExpenseEnabled() != null) {
            existing.setExpenseEnabled(organization.getExpenseEnabled());
        }
        if (organization.getEmployeeCodePrefix() != null) {
            existing.setEmployeeCodePrefix(organization.getEmployeeCodePrefix());
        }
        if (organization.getEmployeeCodePadding() != null) {
            existing.setEmployeeCodePadding(organization.getEmployeeCodePadding());
        }
        if (organization.getNextEmployeeCodeNumber() != null) {
            existing.setNextEmployeeCodeNumber(organization.getNextEmployeeCodeNumber());
        }
        if (organization.getAllowManualEmployeeCodeOverride() != null) {
            existing.setAllowManualEmployeeCodeOverride(organization.getAllowManualEmployeeCodeOverride());
        }
        applyEmployeeCodeDefaults(existing);
        existing.setMaxActiveUsers(requestedLimit);
        return organizationRepository.save(existing);
    }

    @Override
    public void deleteOrganization(Long id) {
        Organization org = getOrganizationById(id);
        List<Employee> employees = userRepository.findAll();
        for (Employee employee : employees) {
            if (employee.getOrganization() != null && employee.getOrganization().getId().equals(org.getId())) {
                employee.setOrganization(null);
            }
        }
        userRepository.saveAll(employees);
        organizationRepository.delete(org);
    }

    @Override
    public Organization setOrganizationActive(Long id, boolean active) {
        Organization org = getOrganizationById(id);
        org.setActive(active);
        return organizationRepository.save(org);
    }

    @Override
    @Transactional
    public int assignEmployeesToOrganization(Long organizationId, List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) return 0;

        Organization organization = getOrganizationById(organizationId);
        List<Employee> employees = userRepository.findAllById(employeeIds);
        long additionalActiveUsers = employees.stream()
                .filter(Employee::getActive)
                .filter(e -> e.getOrganization() == null || !organizationId.equals(e.getOrganization().getId()))
                .count();
        assertCanAddActiveUsers(organizationId, additionalActiveUsers);
        for (Employee employee : employees) {
            employee.setOrganization(organization);
        }
        userRepository.saveAll(employees);
        return employees.size();
    }

    @Override
    public void assertCanAddActiveUsers(Long organizationId, long additionalUsers) {
        if (additionalUsers <= 0) {
            return;
        }
        Organization organization = getOrganizationById(organizationId);
        Integer maxActiveUsers = organization.getMaxActiveUsers();
        validateMaxActiveUsers(maxActiveUsers);
        long currentActiveUsers = userRepository.countByOrganization_IdAndActiveTrue(organizationId);
        long projectedActiveUsers = currentActiveUsers + additionalUsers;
        if (projectedActiveUsers > maxActiveUsers) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Active user limit reached for this organization. Contact ZenyraHR support team to increase limit."
            );
        }
    }

    private void validateMaxActiveUsers(Integer maxActiveUsers) {
        if (maxActiveUsers == null || maxActiveUsers <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Active user limit must be greater than zero");
        }
    }

    private void applyEmployeeCodeDefaults(Organization organization) {
        String prefix = organization.getEmployeeCodePrefix();
        if (prefix == null || prefix.isBlank()) {
            organization.setEmployeeCodePrefix("EMP");
        } else {
            organization.setEmployeeCodePrefix(prefix.trim().toUpperCase());
        }
        Integer padding = organization.getEmployeeCodePadding();
        if (padding == null || padding < 1) {
            organization.setEmployeeCodePadding(4);
        }
        Integer next = organization.getNextEmployeeCodeNumber();
        if (next == null || next < 1) {
            organization.setNextEmployeeCodeNumber(1);
        }
        if (organization.getAllowManualEmployeeCodeOverride() == null) {
            organization.setAllowManualEmployeeCodeOverride(false);
        }
    }
}
