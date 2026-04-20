package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.SalaryCalculationFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryCalculationFormulaRepository extends JpaRepository<SalaryCalculationFormula, Long> {

    List<SalaryCalculationFormula> findByOrganizationIdOrderByIdAsc(Long organizationId);

    Optional<SalaryCalculationFormula> findByIdAndOrganizationId(Long id, Long organizationId);
}

