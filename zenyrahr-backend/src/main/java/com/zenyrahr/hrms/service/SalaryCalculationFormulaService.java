package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.SalaryCalculationFormula;

import java.util.List;

public interface SalaryCalculationFormulaService {
    List<SalaryCalculationFormula> getFormulas(Long organizationId);

    SalaryCalculationFormula createFormula(Long organizationId, SalaryCalculationFormula formula);

    SalaryCalculationFormula updateFormula(Long organizationId, Long id, SalaryCalculationFormula formula);

    void deleteFormula(Long organizationId, Long id);
}
