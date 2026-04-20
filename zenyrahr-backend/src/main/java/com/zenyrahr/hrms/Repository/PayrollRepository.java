package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployeeId(Long employeeId);
    List<Payroll> findByEmployeeIdAndStatus(Long employeeId, String status);

    Optional<Payroll> findByIdAndOrganizationId(Long id, Long organizationId);
}

