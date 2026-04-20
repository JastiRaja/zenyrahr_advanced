package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.EmployeeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeTableRepository extends JpaRepository<EmployeeTable, Long> {
}
