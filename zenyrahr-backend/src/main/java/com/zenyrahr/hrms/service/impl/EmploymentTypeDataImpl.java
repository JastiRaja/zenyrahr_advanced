package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.EmploymentType;
import com.zenyrahr.hrms.Repository.EmploymentTypeRepository;
import com.zenyrahr.hrms.service.EmploymentTypeDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmploymentTypeDataImpl implements EmploymentTypeDataService {

    private final EmploymentTypeRepository employmentTypeRepository;
    private final SequenceService sequenceService;

    @Override
    public EmploymentType createEmploymentType(EmploymentType employmentType) {
        employmentType.setCode(sequenceService.getNextCode("EMPLTYPE"));
        return employmentTypeRepository.save(employmentType);
    }

    @Override
    public Optional<EmploymentType> getEmploymentTypeById(Long id) {
        return employmentTypeRepository.findById(id);
    }

    @Override
    public List<EmploymentType> getAllEmploymentTypes() {
        return employmentTypeRepository.findAll();
    }

    @Override
    public EmploymentType updateEmploymentType(Long id, EmploymentType employmentType) {
        if (employmentTypeRepository.existsById(id)) {
            employmentType.setId(id);
            return employmentTypeRepository.save(employmentType);
        } else {
            throw new RuntimeException("EmploymentType not found");
        }
    }

    @Override
    public void deleteEmploymentType(Long id) {
        employmentTypeRepository.deleteById(id);
    }
}
