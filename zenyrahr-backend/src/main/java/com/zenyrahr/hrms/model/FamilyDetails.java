package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "FamilyDetails")
@Data
public class FamilyDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "family_details_seq")
    @SequenceGenerator(name = "family_details_seq", sequenceName = "family_details_seq", allocationSize = 5)
    private Long id;  // Primary Key

//    @Column(nullable = false, length = 100)
//    private String spouseName;
//
//    @Column(nullable = false, length = 100)
//    private String childrenName;
//
//    @Column(nullable = false)
//    private Integer childrenAge;
//
//    @Column(nullable = false)
//    private Boolean isDependent;  // Whether the child is dependent or not
//
//    @Column(nullable = false, length = 100)
//    private String code;  // Unique code for family details


    private String name;
    private String relationship;
    private String contact;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;


}
