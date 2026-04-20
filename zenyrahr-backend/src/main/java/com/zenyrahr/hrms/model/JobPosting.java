package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_posting",
        indexes = {
                @Index(name = "idx_job_posting_org_status", columnList = "organization_id,status"),
                @Index(name = "idx_job_posting_org_department", columnList = "organization_id,department")
        }
)
@Getter
@Setter
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 100)
    private String department;

    @Column(nullable = false, length = 32)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String sourceChannel;

    @Column(name = "owner_employee_id")
    private Long ownerEmployeeId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @PrePersist
    public void prePersist() {
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
    }
}

