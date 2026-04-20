package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.EmployeeTable;
import com.zenyrahr.hrms.Repository.EmployeeTableRepository;
import com.zenyrahr.hrms.service.EmployeeTableService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeTableServiceImpl implements EmployeeTableService {

    private final EmployeeTableRepository employeeTableRepository;
    private final SequenceService sequenceService; // Assuming you need a sequence code for EmployeeTable

    @Override
    public EmployeeTable createEmployeeTable(EmployeeTable employeeTable) {
        // Generate and set sequence code (if applicable)
        employeeTable.setCode(sequenceService.getNextCode("EMPLOYEE"));
        return employeeTableRepository.save(employeeTable);
    }

    @Override
    public Optional<EmployeeTable> getEmployeeTableById(Long id) {
        return employeeTableRepository.findById(id);
    }

    @Override
    public List<EmployeeTable> getAllEmployeeTables() {
        return employeeTableRepository.findAll();
    }

    @Override
    public EmployeeTable updateEmployeeTable(Long id, EmployeeTable employeeTable) {
        if (employeeTableRepository.existsById(id)) {
            employeeTable.setId(id); // Ensure that the existing ID is retained
            return employeeTableRepository.save(employeeTable);
        } else {
            throw new RuntimeException("Employee record not found");
        }
    }

    @Override
    public void deleteEmployeeTable(Long id) {
        employeeTableRepository.deleteById(id);
    }
}
