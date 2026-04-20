package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "EducationalBackground")
@Data
public class EducationalBackground  extends BaseEntity2{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "educational_background_seq")
    @SequenceGenerator(name = "educational_background_seq", sequenceName = "educational_background_seq", allocationSize = 5)
    private Long id;

//    @Column(nullable = false, length = 100)
    private String highestQualification;

//    @Column(nullable = false, length = 50)
    private String degreeName;

//    @Column(nullable = false, length = 50)
    private String fieldOfStudy;

//    @Column(nullable = false, length = 100)
    private String universityName;

//    @Column(nullable = false)
    private Integer yearOfPassing;

//    @Column(columnDefinition = "TEXT")
    private String certifications;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
