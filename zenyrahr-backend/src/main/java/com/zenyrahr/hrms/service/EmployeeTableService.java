package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.EmployeeTable;

import java.util.List;
import java.util.Optional;

public interface EmployeeTableService {

    EmployeeTable createEmployeeTable(EmployeeTable employeeTable);

    Optional<EmployeeTable> getEmployeeTableById(Long id);

    List<EmployeeTable> getAllEmployeeTables();

    EmployeeTable updateEmployeeTable(Long id, EmployeeTable employeeTable);

    void deleteEmployeeTable(Long id);
}
