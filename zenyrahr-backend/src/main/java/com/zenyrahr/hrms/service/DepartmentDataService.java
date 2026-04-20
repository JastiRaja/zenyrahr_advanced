package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentDataService {

    Department createDepartment(Department department);

    Optional<Department> getDepartmentById(Long id);

    List<Department> getAllDepartments();

    List<Department> getAllDepartmentData();

    Department updateDepartment(Long id, Department department);

    void deleteDepartment(Long id);
}
