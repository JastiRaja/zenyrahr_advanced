package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "work_experience")
@Data
public class WorkExperience extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_experience_seq")
    @SequenceGenerator(name = "work_experience_seq", sequenceName = "work_experience_seq", allocationSize = 1)
    private Long id;  // This is the primary key for WorkExperience entity

    @Column(nullable = false)
    private int totalWorkExperience; // in years

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String rolesAndResponsibilities;

    @Column(nullable = false)
    private String startDate;

    @Column(nullable = false)
    private String endDate;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
