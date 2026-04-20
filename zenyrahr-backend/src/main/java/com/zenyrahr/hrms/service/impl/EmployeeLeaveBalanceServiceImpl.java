package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.EmployeeLeaveBalanceRepository;
import com.zenyrahr.hrms.Repository.LeavePolicySettingRepository;
import com.zenyrahr.hrms.Repository.LeaveTypeRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.dto.CommonLeavePolicyRequest;
import com.zenyrahr.hrms.dto.CommonLeavePolicyResponse;
import com.zenyrahr.hrms.dto.EmployeeLeaveBalanceDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeLeaveBalance;
import com.zenyrahr.hrms.model.LeavePolicyAllocation;
import com.zenyrahr.hrms.model.LeavePolicySetting;
import com.zenyrahr.hrms.model.LeaveType;
import com.zenyrahr.hrms.service.EmployeeLeaveBalanceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RequiredArgsConstructor
@Service
@Transactional
public class EmployeeLeaveBalanceServiceImpl implements EmployeeLeaveBalanceService {

    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final UserDetailsImpl employeeRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeavePolicySettingRepository leavePolicySettingRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public EmployeeLeaveBalance createLeaveBalance(EmployeeLeaveBalance leaveBalance) {
        Employee employee = employeeRepository.getEmployeeById(leaveBalance.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        LeaveType leaveType = leaveBalance.getLeaveType();
        if (leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType).isPresent()) {
            throw new RuntimeException("Leave balance already exists for this leave type.");
        }

        leaveBalance.setEmployee(employee);
        return leaveBalanceRepository.save(leaveBalance);
    }

    @Override
    public List<EmployeeLeaveBalanceDTO> getLeaveBalancesByEmployee(Long employeeId) {
        return leaveBalanceRepository.findByEmployee_Id(employeeId).stream()
                .map(EmployeeLeaveBalanceDTO::new)
                .collect(Collectors.toList());
    }

//    @Override
//    public List<EmployeeLeaveBalance> getAllLeaveBalance(){
//        return leaveBalanceRepository.findAll();
//    }

    @Override
    public List<EmployeeLeaveBalanceDTO> getAllLeaveBalance(){
        return leaveBalanceRepository.findAll().stream()
                .map(EmployeeLeaveBalanceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeLeaveBalance updateLeaveBalance(Long id, Integer newBalance) {
        EmployeeLeaveBalance leaveBalance = leaveBalanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Balance not found"));

        leaveBalance.setBalance(newBalance);
        return leaveBalanceRepository.save(leaveBalance);
    }

    @Override
    public void deleteLeaveBalance(Long id) {
        leaveBalanceRepository.deleteById(id);
    }

    @Override
    public CommonLeavePolicyResponse getCommonLeavePolicy(Employee actor, Long organizationId) {
        Long scopedOrgId = resolveScopedOrganizationId(actor, organizationId);
        LeavePolicySetting policy = leavePolicySettingRepository.findByOrganization_Id(scopedOrgId).orElse(null);
        List<CommonLeavePolicyResponse.Allocation> allocations = new ArrayList<>();
        if (policy != null && policy.getAllocations() != null) {
            allocations = policy.getAllocations().stream()
                    .map(item -> new CommonLeavePolicyResponse.Allocation(
                            item.getLeaveType().getId(),
                            item.getLeaveType().getName(),
                            item.getDays()
                    ))
                    .collect(Collectors.toList());
        } else {
            allocations = leaveTypeRepository.findByOrganization_Id(scopedOrgId).stream()
                    .map(type -> new CommonLeavePolicyResponse.Allocation(
                            type.getId(),
                            type.getName(),
                            type.getDefaultBalance() == null ? 0 : type.getDefaultBalance()
                    ))
                    .collect(Collectors.toList());
        }

        List<Employee> scopedEmployees = tenantAccessService.isMainAdmin(actor)
                ? userRepository.findByOrganization_Id(scopedOrgId)
                : tenantAccessService.filterEmployeesByScope(actor, userRepository.findAll());
        int employeeCount = scopedEmployees.size();
        return new CommonLeavePolicyResponse(
                policy != null ? policy.getYearMode() : "CALENDAR",
                policy != null ? policy.getYearStart() : java.time.LocalDate.now().getYear(),
                employeeCount,
                allocations
        );
    }

    @Override
    public CommonLeavePolicyResponse applyCommonLeavePolicy(Employee actor, Long organizationId, CommonLeavePolicyRequest request) {
        if (request == null || request.getAllocations() == null || request.getAllocations().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one leave allocation is required");
        }
        String yearMode = request.getYearMode() == null ? "CALENDAR" : request.getYearMode().trim().toUpperCase();
        if (!yearMode.equals("CALENDAR") && !yearMode.equals("FINANCIAL")) {
            throw new ResponseStatusException(BAD_REQUEST, "Year mode must be CALENDAR or FINANCIAL");
        }
        if (request.getYearStart() == null || request.getYearStart() < 2000 || request.getYearStart() > 2100) {
            throw new ResponseStatusException(BAD_REQUEST, "Please provide a valid year");
        }

        Long scopedOrgId = resolveScopedOrganizationId(actor, organizationId);
        LeavePolicySetting policy = leavePolicySettingRepository.findByOrganization_Id(scopedOrgId).orElse(null);
        if (policy == null) {
            policy = new LeavePolicySetting();
            policy.setOrganization(actor.getOrganization() != null && actor.getOrganization().getId() != null
                    ? actor.getOrganization()
                    : organizationRepository.findById(scopedOrgId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Organization not found")));
        }
        policy.setYearMode(yearMode);
        policy.setYearStart(request.getYearStart());
        policy.getAllocations().clear();

        List<Employee> scopedEmployees = tenantAccessService.isMainAdmin(actor)
                ? userRepository.findByOrganization_Id(scopedOrgId)
                : tenantAccessService.filterEmployeesByScope(actor, userRepository.findAll());

        for (CommonLeavePolicyRequest.AllocationItem item : request.getAllocations()) {
            if (item.getLeaveTypeId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Leave type is required");
            }
            int days = item.getDays() == null ? 0 : item.getDays();
            if (days < 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Leave days cannot be negative");
            }

            LeaveType leaveType = leaveTypeRepository.findByIdAndOrganization_Id(item.getLeaveTypeId(), scopedOrgId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Leave type not found in organization"));

            LeavePolicyAllocation allocation = new LeavePolicyAllocation();
            allocation.setPolicy(policy);
            allocation.setLeaveType(leaveType);
            allocation.setDays(days);
            policy.getAllocations().add(allocation);

            for (Employee employee : scopedEmployees) {
                upsertEmployeeLeaveBalance(employee, leaveType, days);
            }
        }

        leavePolicySettingRepository.save(policy);
        return getCommonLeavePolicy(actor, scopedOrgId);
    }

    @Override
    public void initializeLeaveBalancesForEmployee(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return;
        }
        LeavePolicySetting policy = resolvePolicyForEmployee(employee);
        if (policy != null && policy.getAllocations() != null && !policy.getAllocations().isEmpty()) {
            for (LeavePolicyAllocation allocation : policy.getAllocations()) {
                upsertEmployeeLeaveBalance(employee, allocation.getLeaveType(), allocation.getDays());
            }
            return;
        }

        // Fallback to default leave type balances when no common policy is configured yet.
        Long orgId = employee.getOrganization() != null ? employee.getOrganization().getId() : null;
        if (orgId == null) {
            return;
        }
        for (LeaveType type : leaveTypeRepository.findByOrganization_Id(orgId)) {
            int defaultDays = type.getDefaultBalance() == null ? 0 : type.getDefaultBalance();
            if (defaultDays > 0) {
                upsertEmployeeLeaveBalance(employee, type, defaultDays);
            }
        }
    }

    private void upsertEmployeeLeaveBalance(Employee employee, LeaveType leaveType, int days) {
        EmployeeLeaveBalance balance = leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseGet(EmployeeLeaveBalance::new);
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setBalance(days);
        leaveBalanceRepository.save(balance);
    }

    private LeavePolicySetting resolvePolicyForEmployee(Employee employee) {
        if (employee.getOrganization() != null && employee.getOrganization().getId() != null) {
            return leavePolicySettingRepository.findByOrganization_Id(employee.getOrganization().getId()).orElse(null);
        }
        return null;
    }

    private Long resolveScopedOrganizationId(Employee actor, Long organizationId) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (organizationId == null) {
                throw new ResponseStatusException(BAD_REQUEST, "organizationId is required for main admin");
            }
            return organizationId;
        }
        return tenantAccessService.requireOrganizationId(actor);
    }
}