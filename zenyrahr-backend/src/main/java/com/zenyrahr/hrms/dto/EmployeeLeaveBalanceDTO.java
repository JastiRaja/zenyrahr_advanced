package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.EmployeeLeaveBalance;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeLeaveBalanceDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer balance;

    public EmployeeLeaveBalanceDTO(EmployeeLeaveBalance leaveBalance) {
        this.id = leaveBalance.getId();
        this.employeeId = leaveBalance.getEmployee().getId();
        this.employeeName = leaveBalance.getEmployee().getName();
        this.leaveTypeId = leaveBalance.getLeaveType().getId();
        this.leaveTypeName = leaveBalance.getLeaveType().getName();
        this.balance = leaveBalance.getBalance();
    }
}
