package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_job_history")
public class EmployeeJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_job_history_seq")
    @SequenceGenerator(name = "employee_job_history_seq", sequenceName = "employee_job_history_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private Employee changedBy;

    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @Column(name = "old_role", length = 50)
    private String oldRole;

    @Column(name = "new_role", length = 50)
    private String newRole;

    @Column(name = "old_position", length = 100)
    private String oldPosition;

    @Column(name = "new_position", length = 100)
    private String newPosition;

    @Column(name = "old_department", length = 100)
    private String oldDepartment;

    @Column(name = "new_department", length = 100)
    private String newDepartment;

    @Column(name = "old_work_location", length = 100)
    private String oldWorkLocation;

    @Column(name = "new_work_location", length = 100)
    private String newWorkLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_manager_id")
    private Employee oldManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_manager_id")
    private Employee newManager;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
