package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.model.BaseEntity2;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "timesheet")
@Data
@Getter
@Setter
public class Timesheet extends BaseEntity2{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal hoursWorked;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String description;

    @Column(name="required_comments")
    private String requiredComments;

    @Enumerated(EnumType.STRING)
    private TimeSheetStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 1;

    @Column(name = "max_approval_level", nullable = false)
    private Integer maxApprovalLevel = 1;

    @Transient
    private Boolean canCurrentUserApprove = false;
}