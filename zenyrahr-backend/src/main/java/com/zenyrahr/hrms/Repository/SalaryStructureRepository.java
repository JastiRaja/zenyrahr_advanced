package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    SalaryStructure findByEmployeeId(Long employeeId);
}