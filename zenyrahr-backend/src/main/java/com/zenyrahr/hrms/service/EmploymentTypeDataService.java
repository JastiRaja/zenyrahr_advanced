package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.EmploymentType;

import java.util.List;
import java.util.Optional;

public interface EmploymentTypeDataService {

    EmploymentType createEmploymentType(EmploymentType employmentType);

    Optional<EmploymentType> getEmploymentTypeById(Long id);

    List<EmploymentType> getAllEmploymentTypes();

    EmploymentType updateEmploymentType(Long id, EmploymentType employmentType);

    void deleteEmploymentType(Long id);
}
