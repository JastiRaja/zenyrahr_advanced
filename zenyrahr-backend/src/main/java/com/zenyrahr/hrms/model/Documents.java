package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "employee_documents")
public class Documents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="profile_image_url")
    private String profileImageUrl;

    @Column(name="education_documents")
    private String educationDocumentsUrl;

    @Column(name="personal_documents")
    private String personalDocumentsUrl;

    @Column(name="medical_documents")
    private String medicalDocumentsUrl;

    @Column(name="experience_documents")
    private String experienceDocumentsUrl;

    @OneToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
