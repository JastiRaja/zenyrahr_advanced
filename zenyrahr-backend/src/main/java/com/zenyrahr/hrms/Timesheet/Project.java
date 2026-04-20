package com.zenyrahr.hrms.Timesheet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "project")
@Data
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq")
    @SequenceGenerator(name = "project_seq", sequenceName = "project_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false, length = 100)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"logoUrl", "address"})
    private Organization organization;

    @ManyToMany(mappedBy = "projects")
    @JsonIgnoreProperties({"projects", "education", "experience", "familyDetails", "medicalRecords", "leaveBalances", "documents", "reportingManager", "organization"})
    private List<Employee> employees;

    @Column(nullable = false, length = 32)
    private String status; // ACTIVE, COMPLETED
}
