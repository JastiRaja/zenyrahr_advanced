package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "EmployeeTable")
@Data
public class EmployeeTable extends BaseEntity2{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "employee_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 50)
    private String jobType;

    @Column(nullable = false, length = 50)
    private String employeeStatus;

    @Column(nullable = false)
    private LocalDate dateOfJoin;

    @Column(nullable = true, length = 100)
    private String reportingManager; // No reference to another table, just a string

    @Column(nullable = false, length = 100)
    private String workLocation;

    @Column(nullable = false, length = 100)
    private String employeeName; // Storing employee name directly as a string (if needed)
}
