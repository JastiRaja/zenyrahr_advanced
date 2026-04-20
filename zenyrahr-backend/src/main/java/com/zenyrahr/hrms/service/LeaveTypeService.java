package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.LeaveType;

import java.util.List;

public interface LeaveTypeService {
    LeaveType createLeaveType(Employee actor, Long organizationId, LeaveType leaveType);

    List<LeaveType> getAllLeaveTypes(Employee actor, Long organizationId);

    LeaveType getLeaveTypeById(Employee actor, Long organizationId, Long id);

    LeaveType updateLeaveType(Employee actor, Long organizationId, Long id, LeaveType leaveType);

    void deleteLeaveType(Employee actor, Long organizationId, Long id);
}
