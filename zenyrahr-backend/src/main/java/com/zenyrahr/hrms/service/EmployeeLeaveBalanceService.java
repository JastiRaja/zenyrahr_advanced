package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.dto.EmployeeLeaveBalanceDTO;
import com.zenyrahr.hrms.dto.CommonLeavePolicyRequest;
import com.zenyrahr.hrms.dto.CommonLeavePolicyResponse;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeLeaveBalance;

import java.util.List;

public interface EmployeeLeaveBalanceService {
    EmployeeLeaveBalance createLeaveBalance(EmployeeLeaveBalance leaveBalance);

    List<EmployeeLeaveBalanceDTO> getLeaveBalancesByEmployee(Long employeeId);

    EmployeeLeaveBalance updateLeaveBalance(Long id, Integer newBalance);

    void deleteLeaveBalance(Long id);

//    @EntityGraph(attributePaths = {"employee", "leaveType"})
    List<EmployeeLeaveBalanceDTO> getAllLeaveBalance();

    CommonLeavePolicyResponse getCommonLeavePolicy(Employee actor, Long organizationId);

    CommonLeavePolicyResponse applyCommonLeavePolicy(Employee actor, Long organizationId, CommonLeavePolicyRequest request);

    void initializeLeaveBalancesForEmployee(Employee employee);


}
