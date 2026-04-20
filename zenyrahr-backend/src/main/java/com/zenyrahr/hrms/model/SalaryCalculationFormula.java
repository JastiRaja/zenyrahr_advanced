package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "SalaryCalculationFormula")
@Entity
public class SalaryCalculationFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant that owns this formula (required for new rows). */
    @Column(name = "organization_id")
    private Long organizationId;

    // Base Salary Percentage for Gross Earnings
    @Column(nullable = false)
    private Double basicToGrossPercentage;

    // HRA Percentage of Basic Salary
    @Column(nullable = false)
    private Double hraPercentage;

    // Conveyance Percentage of Basic Salary
    @Column(nullable = false)
    private Double conveyancePercentage;

    // EPF Percentage of Basic Salary
    @Column(nullable = false)
    private Double epfPercentage;

    // Professional Tax (fixed value)
    @Column(nullable = false)
    private Double professionalTax;

    // Medical Allowance (fixed value)
    @Column(nullable = false)
    private Double medicalAllowance;

    // Health Insurance Deduction (fixed value or percentage)
    @Column(nullable = false)
    private Double healthInsuranceDeduction;
}

