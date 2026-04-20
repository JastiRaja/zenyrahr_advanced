package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "IdentificationDetails")
@Data
public class IdentificationDetails extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long Id;

    @Column(nullable = false, unique = true)
    private String aadharNumber; // Unique ID (Aadhar or SSN)

    @Column(nullable = false, unique = true)
    private String panCard; // PAN card or Tax ID

    @Column(nullable = false, unique = true)
    private String passportNumber; // Passport number

    @Column(nullable = false, unique = true)
    private String drivingLicense; // Driving license number

    @Column(nullable = false)
    private String idProofDocuments; // File paths or references to uploaded ID documents

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
