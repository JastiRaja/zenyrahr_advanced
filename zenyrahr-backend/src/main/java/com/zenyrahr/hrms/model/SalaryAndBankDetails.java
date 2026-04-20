package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.zenyrahr.hrms.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "SalaryAndBankDetails")
@Data
public class SalaryAndBankDetails extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Use IDENTITY to auto-generate the primary key
    private Long id;

    @Column(nullable = false)
    private Double ctc;

    @Column(nullable = false)
    private Double basic;

    @Column(nullable = false)
    private Double allowances;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String accountNumber;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String ifscCode;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String accountHolderName;

    @Column(nullable = false)
    private String paymentMethod;

    private String code; // Unique code for Salary and Bank Details, can be generated using Sequence Service

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;

    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String uanNumber;

    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String panNumber;

    @Column
    private String companyName;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String companyAddress;

    @Column
    private String companyLogoUrl;
}
