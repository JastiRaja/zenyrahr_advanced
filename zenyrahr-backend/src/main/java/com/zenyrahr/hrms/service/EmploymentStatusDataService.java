package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.EmploymentStatus;

import java.util.List;
import java.util.Optional;

public interface EmploymentStatusDataService {

    EmploymentStatus createEmploymentStatus(EmploymentStatus employmentStatus);

    Optional<EmploymentStatus> getEmploymentStatusById(Long id);

    List<EmploymentStatus> getAllEmploymentStatuses();

    EmploymentStatus updateEmploymentStatus(Long id, EmploymentStatus employmentStatus);

    void deleteEmploymentStatus(Long id);
}
