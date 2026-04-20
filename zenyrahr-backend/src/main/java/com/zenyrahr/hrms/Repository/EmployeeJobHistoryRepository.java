package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.EmployeeJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeJobHistoryRepository extends JpaRepository<EmployeeJobHistory, Long> {
    List<EmployeeJobHistory> findByEmployee_IdOrderByChangedAtDesc(Long employeeId);
}
