package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "payroll_generation")
public class PayrollGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant scope: each organization has its own payroll run per calendar month (required for new rows). */
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(nullable = false)
    private String monthYear; // Format: YYYY-MM

    @Column(nullable = false)
    private LocalDate generationDate;

    @Column(nullable = false)
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED

    @Column(nullable = false)
    private String generatedBy;

    @Column
    private String approvedBy;

    @Column
    private LocalDate approvedDate;

    @Column
    private String rejectionReason;

    @Column(nullable = false)
    private Integer totalEmployees;

    @Column(nullable = false)
    private Double totalPayrollAmount;

    @Column(nullable = false)
    private Double totalDeductions;

    @Column(nullable = false)
    private Double totalNetAmount;

    @Column
    private String remarks;
} 