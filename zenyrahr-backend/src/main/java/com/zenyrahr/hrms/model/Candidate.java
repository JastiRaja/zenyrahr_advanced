package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "candidate",
        indexes = {
                @Index(name = "idx_candidate_org_stage", columnList = "organization_id,stage"),
                @Index(name = "idx_candidate_org_job", columnList = "organization_id,job_posting_id")
        }
)
@Getter
@Setter
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecruitmentStage stage = RecruitmentStage.APPLIED;

    @Column(length = 80)
    private String source;

    @Column(name = "owner_employee_id")
    private Long ownerEmployeeId;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "stage_updated_at", nullable = false)
    private LocalDateTime stageUpdatedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (appliedAt == null) {
            appliedAt = now;
        }
        if (stageUpdatedAt == null) {
            stageUpdatedAt = now;
        }
    }
}

