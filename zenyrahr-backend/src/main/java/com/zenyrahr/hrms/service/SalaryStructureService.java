package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.SalaryStructure;

import java.util.List;


public interface SalaryStructureService {
    List<SalaryStructure> getAllSalaryStructures();

    SalaryStructure createSalaryStructure(SalaryStructure salaryStructure);

    SalaryStructure updateSalaryStructure(Long id, SalaryStructure salaryStructure);

    void deleteSalaryStructure(Long id);

}
