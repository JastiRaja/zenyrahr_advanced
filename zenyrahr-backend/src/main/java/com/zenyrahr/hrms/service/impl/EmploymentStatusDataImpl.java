package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.EmploymentStatus;
import com.zenyrahr.hrms.Repository.EmploymentStatusRepository;
import com.zenyrahr.hrms.service.EmploymentStatusDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmploymentStatusDataImpl implements EmploymentStatusDataService {

    private final EmploymentStatusRepository employmentStatusRepository;
    private final SequenceService sequenceService;

    @Override
    public EmploymentStatus createEmploymentStatus(EmploymentStatus employmentStatus) {
        employmentStatus.setCode(sequenceService.getNextCode("EMPLSTAT"));
        return employmentStatusRepository.save(employmentStatus);
    }

    @Override
    public Optional<EmploymentStatus> getEmploymentStatusById(Long id) {
        return employmentStatusRepository.findById(id);
    }

    @Override
    public List<EmploymentStatus> getAllEmploymentStatuses() {
        return employmentStatusRepository.findAll();
    }

    @Override
    public EmploymentStatus updateEmploymentStatus(Long id, EmploymentStatus employmentStatus) {
        if (employmentStatusRepository.existsById(id)) {
            employmentStatus.setId(id);
            return employmentStatusRepository.save(employmentStatus);
        } else {
            throw new RuntimeException("EmploymentStatus not found");
        }
    }

    @Override
    public void deleteEmploymentStatus(Long id) {
        employmentStatusRepository.deleteById(id);
    }
}
