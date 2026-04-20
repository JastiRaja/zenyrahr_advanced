package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Table(name = "SalaryStructure")
@Entity
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private BigDecimal baseSalary;

    private BigDecimal houseRentAllowance;
    private BigDecimal conveyanceAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal otherAllowances;
    private BigDecimal epfAmount;
    private BigDecimal professionalTax;
    private BigDecimal healthInsurance;

    @ManyToOne
    @JoinColumn(name = "formula_id", nullable = false)
    private SalaryCalculationFormula formula;
}