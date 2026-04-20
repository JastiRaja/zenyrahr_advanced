package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "AdditionalInformation")
@Data
public class AdditionalInformation extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "additional_info_seq")
    @SequenceGenerator(name = "additional_info_seq", sequenceName = "additional_info_seq", allocationSize = 5)
    private Long id;  // Primary Key

    @Column(length = 50)
    private String workPreferences;  // Remote, Hybrid, On-site

    @Column(columnDefinition = "TEXT")
    private String languagesKnown;  // Languages known and their proficiency levels

    @Column(length = 200)
    private String linkedinProfile;  // LinkedIn URL or Personal Portfolio URL


    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
