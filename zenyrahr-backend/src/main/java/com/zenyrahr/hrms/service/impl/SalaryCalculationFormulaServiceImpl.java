package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.SalaryCalculationFormulaRepository;
import com.zenyrahr.hrms.model.SalaryCalculationFormula;
import com.zenyrahr.hrms.service.SalaryCalculationFormulaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryCalculationFormulaServiceImpl implements SalaryCalculationFormulaService {

    private final SalaryCalculationFormulaRepository formulaRepository;

    public SalaryCalculationFormulaServiceImpl(SalaryCalculationFormulaRepository formulaRepository) {
        this.formulaRepository = formulaRepository;
    }

    @Override
    public List<SalaryCalculationFormula> getFormulas(Long organizationId) {
        return formulaRepository.findByOrganizationIdOrderByIdAsc(organizationId);
    }

    @Override
    public SalaryCalculationFormula createFormula(Long organizationId, SalaryCalculationFormula formula) {
        formula.setId(null);
        formula.setOrganizationId(organizationId);
        return formulaRepository.save(formula);
    }

    @Override
    public SalaryCalculationFormula updateFormula(Long organizationId, Long id, SalaryCalculationFormula incoming) {
        SalaryCalculationFormula existing = formulaRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Salary formula not found"));
        existing.setBasicToGrossPercentage(incoming.getBasicToGrossPercentage());
        existing.setHraPercentage(incoming.getHraPercentage());
        existing.setConveyancePercentage(incoming.getConveyancePercentage());
        existing.setEpfPercentage(incoming.getEpfPercentage());
        existing.setProfessionalTax(incoming.getProfessionalTax());
        existing.setMedicalAllowance(incoming.getMedicalAllowance());
        existing.setHealthInsuranceDeduction(incoming.getHealthInsuranceDeduction());
        return formulaRepository.save(existing);
    }

    @Override
    public void deleteFormula(Long organizationId, Long id) {
        SalaryCalculationFormula existing = formulaRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Salary formula not found"));
        formulaRepository.delete(existing);
    }
}
