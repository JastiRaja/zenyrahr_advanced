package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Table(name = "Payroll", uniqueConstraints = {@UniqueConstraint(columnNames = {"employeeId", "payrollMonthYear"})})
@Entity
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Denormalized tenant key for defense-in-depth queries and reporting. */
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate payrollDate; // Ensure this is mapped from JSON

    @Column(nullable = false)
    private String payrollMonthYear;

    @PrePersist
    @PreUpdate
    public void updatePayrollMonthYear() {
        // Double-check that payrollDate is not null
        if (this.payrollDate == null) {
            throw new IllegalArgumentException("payrollDate must not be null");
        }
        this.payrollMonthYear = payrollDate.getYear() + "-" + String.format("%02d", payrollDate.getMonthValue());
    }

    private String status; // PENDING, APPROVED, REJECTED

    // Earnings
    private String grossPay;
    private String netPay;
    private String netPayInWords;
    private String totalEarnings;

    private String basicPay;
    private String houseRentAllowance;
    /** Dearness allowance from payscale (prorated). */
    private String dearnessAllowance;
    private String conveyanceAllowance;
    private String medicalAllowance;
    private String otherAllowances;

    // Deductions
    private String totalDeductions;
    private String epfAmount;
    private String professionalTax;
    private String healthInsuranceDeduction;

    // Working Days Information
    private String workingDays;
    private String totalWorkingDays;
    private String presentDays;
    private String absentDays;
    private String halfDays;
    private String paidDays;
}