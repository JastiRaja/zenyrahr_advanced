package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
//import javax.persistence.*;

@Data
@Entity
@Table(name = "payscale", uniqueConstraints = @UniqueConstraint(columnNames = "employee_id"))
public class Payscale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Double ctc; // Cost to Company

    @Column(nullable = false)
    private Double basicSalary;

    @Column(nullable = false)
    private Double hra; // House Rent Allowance

    @Column(nullable = false)
    private Double da; // Dearness Allowance

    @Column(nullable = false)
    private Double allowance; // All other allowances

    @Column(nullable = false)
    private Double medicalAllowance;

    @Column(nullable = false)
    private Double pfContribution; // Employee's PF contribution

    @Column(nullable = false)
    private Double professionalTax;

    @Column(nullable = false)
    private Double healthInsurance;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private String status; // ACTIVE, INACTIVE

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Column
    private String updatedBy;

    @Column
    private LocalDate updatedAt;

    // You can add more fields as needed, such as effective date, etc.
} 