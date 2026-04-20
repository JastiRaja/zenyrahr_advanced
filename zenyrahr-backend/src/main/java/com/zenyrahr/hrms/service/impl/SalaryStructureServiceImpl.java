package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.SalaryStructureRepository;
import com.zenyrahr.hrms.model.SalaryStructure;
import com.zenyrahr.hrms.service.SalaryStructureService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;

    public SalaryStructureServiceImpl(SalaryStructureRepository salaryStructureRepository) {
        this.salaryStructureRepository = salaryStructureRepository;
    }

    @Override
    public List<SalaryStructure> getAllSalaryStructures() {
        return salaryStructureRepository.findAll();
    }

    @Override
    public SalaryStructure createSalaryStructure(SalaryStructure salaryStructure) {
        return salaryStructureRepository.save(salaryStructure);
    }

    @Override
    public SalaryStructure updateSalaryStructure(Long id, SalaryStructure salaryStructure) {
        salaryStructure.setId(id);
        return salaryStructureRepository.save(salaryStructure);
    }

    @Override
    public void deleteSalaryStructure(Long id) {
        salaryStructureRepository.deleteById(id);
    }
}
