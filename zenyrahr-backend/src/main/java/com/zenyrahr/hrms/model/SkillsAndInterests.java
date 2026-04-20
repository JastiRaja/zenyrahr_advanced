//package com.talvox.hrms.model;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Table(name = "SkillsAndInterests")
//@Data
//public class SkillsAndInterests  {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skills_interests_seq")
//    @SequenceGenerator(name = "skills_interests_seq", sequenceName = "skills_interests_seq", allocationSize = 5)
//    private Long id;  // Primary Key
//
//    @Column(columnDefinition = "TEXT")
//    private String technicalSkills;  // Programming languages, tools, etc.
//
//    @Column(columnDefinition = "TEXT")
//    private String softSkills;  // Communication, leadership, etc.
//
//    @Column(columnDefinition = "TEXT")
//    private String hobbiesAndInterests;  // Hobbies and personal interests
//
//    @ManyToOne
//    @JoinColumn(name = "employee_id")
//    @JsonBackReference
//    private Employee employee;
//}
