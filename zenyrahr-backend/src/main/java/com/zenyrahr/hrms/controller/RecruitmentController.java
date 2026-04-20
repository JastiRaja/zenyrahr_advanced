package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Candidate;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.JobPosting;
import com.zenyrahr.hrms.service.RecruitmentService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final TenantAccessService tenantAccessService;
    private final RecruitmentService recruitmentService;

    @GetMapping("/jobs")
    public ResponseEntity<List<JobPostingResponse>> getJobPostings(
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        List<JobPostingResponse> rows = recruitmentService.listJobPostings(actor, organizationId).stream()
                .map(this::toJobPostingResponse)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateResponse>> getCandidates(
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        List<CandidateResponse> rows = recruitmentService.listCandidates(actor, organizationId).stream()
                .map(this::toCandidateResponse)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobPostingResponse> createJobPosting(
            @RequestBody JobPostingCreateRequest payload,
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        JobPosting saved = recruitmentService.createJobPosting(
                actor,
                organizationId,
                payload.title(),
                payload.department(),
                payload.status(),
                payload.description(),
                payload.sourceChannel(),
                payload.ownerEmployeeId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toJobPostingResponse(saved));
    }

    @PostMapping("/candidates")
    public ResponseEntity<CandidateResponse> createCandidate(
            @RequestBody CandidateCreateRequest payload,
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Candidate saved = recruitmentService.createCandidate(
                actor,
                organizationId,
                payload.fullName(),
                payload.email(),
                payload.stage(),
                payload.jobPostingId(),
                payload.source(),
                payload.notes(),
                payload.ownerEmployeeId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toCandidateResponse(saved));
    }

    @PutMapping("/candidates/{id}/stage")
    public ResponseEntity<CandidateResponse> transitionCandidateStage(
            @PathVariable Long id,
            @RequestBody CandidateStageTransitionRequest payload,
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Candidate updated = recruitmentService.transitionCandidateStage(
                actor,
                organizationId,
                id,
                payload.stage(),
                payload.notes(),
                payload.rejectionReason()
        );
        return ResponseEntity.ok(toCandidateResponse(updated));
    }

    private JobPostingResponse toJobPostingResponse(JobPosting entity) {
        return new JobPostingResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDepartment(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getSourceChannel(),
                entity.getOwnerEmployeeId(),
                entity.getOpenedAt() != null ? entity.getOpenedAt().toString() : null,
                entity.getClosedAt() != null ? entity.getClosedAt().toString() : null
        );
    }

    private CandidateResponse toCandidateResponse(Candidate entity) {
        return new CandidateResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getStage() != null ? entity.getStage().name() : null,
                entity.getSource(),
                entity.getOwnerEmployeeId(),
                entity.getAppliedAt() != null ? entity.getAppliedAt().toString() : null,
                entity.getStageUpdatedAt() != null ? entity.getStageUpdatedAt().toString() : null,
                entity.getNotes(),
                entity.getRejectionReason()
        );
    }

    public record JobPostingResponse(
            Long id,
            String title,
            String department,
            String status,
            String description,
            String sourceChannel,
            Long ownerEmployeeId,
            String openedAt,
            String closedAt
    ) {}

    public record CandidateResponse(
            Long id,
            String fullName,
            String email,
            String stage,
            String source,
            Long ownerEmployeeId,
            String appliedAt,
            String stageUpdatedAt,
            String notes,
            String rejectionReason
    ) {}

    public record JobPostingCreateRequest(
            String title,
            String department,
            String status,
            String description,
            String sourceChannel,
            Long ownerEmployeeId
    ) {}

    public record CandidateCreateRequest(
            String fullName,
            String email,
            String stage,
            Long jobPostingId,
            String source,
            String notes,
            Long ownerEmployeeId
    ) {}

    public record CandidateStageTransitionRequest(
            String stage,
            String notes,
            String rejectionReason
    ) {}
}

