package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeLeaveBalance;
import com.zenyrahr.hrms.model.LeaveType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {
    Optional<EmployeeLeaveBalance> findByEmployeeAndLeaveType(Employee employee, LeaveType leaveType);

//    List<EmployeeLeaveBalance> findByEmployee(Employee employee);

    @EntityGraph(attributePaths = {"employee", "leaveType"})
    List<EmployeeLeaveBalance> findAll();

    List<EmployeeLeaveBalance> findByEmployee_Id(Long employeeId);
//Optional<EmployeeLeaveBalance> findByEmployee_IdAndLeaveType_Id(Long employeeId, Long leaveTypeId);
}

