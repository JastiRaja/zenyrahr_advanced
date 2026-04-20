package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.LeaveRequest;
import com.zenyrahr.hrms.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployee_Id(Long employeeId);

    List<LeaveRequest> findByEmployee_IdAndStatus(Long employeeId, LeaveStatus status);
}
