package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "JobInformation")
@Data
public class JobInformation extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key representing EmployeeID

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String employmentStatus;

    @Column(nullable = false)
    private String dateOfJoining;

    @Column(nullable = false)
    private String reportingManager;

    @Column(nullable = false)
    private String workLocation;

    @Column(nullable = false)
    private String employeeGrade;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
