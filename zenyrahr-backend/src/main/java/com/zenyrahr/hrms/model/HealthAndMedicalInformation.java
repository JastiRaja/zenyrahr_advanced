package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "HealthAndMedicalInformation")
@Data
public class HealthAndMedicalInformation extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "health_medical_info_seq")
    @SequenceGenerator(name = "health_medical_info_seq", sequenceName = "health_medical_info_seq", allocationSize = 5)
    private Long id;  // Primary Key

    @Column(nullable = false, length = 10)
    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String allergies;  // List of allergies (if any)

    @Column(columnDefinition = "TEXT")
    private String preExistingConditions;  // Pre-existing medical conditions

    // Medical Insurance Details
    @Column(nullable = false, length = 100)
    private String providerName;

    @Column(nullable = false, length = 50)
    private String policyNumber;

    @Column(nullable = false)
    private String validity;  // Validity of the insurance policy

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
