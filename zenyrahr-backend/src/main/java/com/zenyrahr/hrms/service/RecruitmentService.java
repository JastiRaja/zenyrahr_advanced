package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.CandidateRepository;
import com.zenyrahr.hrms.Repository.JobPostingRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.model.Candidate;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.JobPosting;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.model.RecruitmentStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final TenantAccessService tenantAccessService;
    private final OrganizationRepository organizationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateRepository candidateRepository;
    private static final Map<RecruitmentStage, Set<RecruitmentStage>> ALLOWED_STAGE_TRANSITIONS = Map.of(
            RecruitmentStage.APPLIED, Set.of(RecruitmentStage.SHORTLISTED, RecruitmentStage.REJECTED),
            RecruitmentStage.SHORTLISTED, Set.of(RecruitmentStage.INTERVIEW, RecruitmentStage.REJECTED),
            RecruitmentStage.INTERVIEW, Set.of(RecruitmentStage.OFFERED, RecruitmentStage.REJECTED),
            RecruitmentStage.OFFERED, Set.of(RecruitmentStage.HIRED, RecruitmentStage.REJECTED),
            RecruitmentStage.HIRED, Set.of(),
            RecruitmentStage.REJECTED, Set.of()
    );

    public List<JobPosting> listJobPostings(Employee actor, Long organizationId) {
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return jobPostingRepository.findByOrganization_IdOrderByIdDesc(scopedOrgId);
    }

    public List<Candidate> listCandidates(Employee actor, Long organizationId, String stage) {
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        RecruitmentStage scopedStage = normalizeStageFilter(stage);
        if (scopedStage == null) {
            return candidateRepository.findByOrganization_IdOrderByIdDesc(scopedOrgId);
        }
        return candidateRepository.findByOrganization_IdAndStageOrderByIdDesc(scopedOrgId, scopedStage);
    }

    public JobPosting createJobPosting(
            Employee actor,
            Long organizationId,
            String title,
            String department,
            String status,
            String description,
            String sourceChannel,
            Long ownerEmployeeId
    ) {
        tenantAccessService.assertCanManageEmployees(actor);
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        Organization organization = organizationRepository.findById(scopedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Job title is required");
        }

        JobPosting posting = new JobPosting();
        posting.setTitle(normalizedTitle);
        posting.setDepartment(toNullableTrimmed(department));
        posting.setStatus(normalizeJobStatus(status));
        posting.setDescription(toNullableTrimmed(description));
        posting.setSourceChannel(toNullableTrimmed(sourceChannel));
        posting.setOwnerEmployeeId(resolveOwnerEmployeeId(actor, ownerEmployeeId));
        posting.setOrganization(organization);
        return jobPostingRepository.save(posting);
    }

    public Candidate createCandidate(
            Employee actor,
            Long organizationId,
            String fullName,
            String email,
            String stage,
            Long jobPostingId,
            String source,
            String notes,
            Long ownerEmployeeId
    ) {
        tenantAccessService.assertCanManageEmployees(actor);
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        Organization organization = organizationRepository.findById(scopedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        String normalizedName = fullName == null ? "" : fullName.trim();
        if (normalizedName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Candidate full name is required");
        }

        JobPosting jobPosting = null;
        if (jobPostingId != null) {
            jobPosting = jobPostingRepository.findByIdAndOrganization_Id(jobPostingId, scopedOrgId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job posting not found for this organization"));
        }

        Candidate candidate = new Candidate();
        candidate.setFullName(normalizedName);
        candidate.setEmail(toNullableTrimmed(email));
        candidate.setStage(normalizeStage(stage));
        candidate.setSource(toNullableTrimmed(source));
        candidate.setNotes(toNullableTrimmed(notes));
        candidate.setOwnerEmployeeId(resolveOwnerEmployeeId(actor, ownerEmployeeId));
        candidate.setJobPosting(jobPosting);
        candidate.setOrganization(organization);
        return candidateRepository.save(candidate);
    }

    public Candidate transitionCandidateStage(
            Employee actor,
            Long organizationId,
            Long candidateId,
            String nextStage,
            String notes,
            String rejectionReason
    ) {
        tenantAccessService.assertCanManageEmployees(actor);
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        Candidate candidate = candidateRepository.findByIdAndOrganization_Id(candidateId, scopedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found for this organization"));

        RecruitmentStage targetStage = normalizeStage(nextStage);
        RecruitmentStage currentStage = candidate.getStage();
        if (currentStage == null) {
            currentStage = RecruitmentStage.APPLIED;
        }
        if (currentStage == targetStage) {
            throw new ResponseStatusException(BAD_REQUEST, "Candidate is already in stage " + targetStage.name());
        }

        Set<RecruitmentStage> allowedTargets = ALLOWED_STAGE_TRANSITIONS.getOrDefault(currentStage, Set.of());
        if (!allowedTargets.contains(targetStage)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid stage transition from " + currentStage.name() + " to " + targetStage.name()
            );
        }

        candidate.setStage(targetStage);
        candidate.setStageUpdatedAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.trim().isBlank()) {
            candidate.setNotes(notes.trim());
        }
        if (targetStage == RecruitmentStage.REJECTED) {
            String normalizedReason = toNullableTrimmed(rejectionReason);
            if (normalizedReason == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Rejection reason is required when moving to REJECTED");
            }
            candidate.setRejectionReason(normalizedReason);
        } else {
            candidate.setRejectionReason(null);
        }
        return candidateRepository.save(candidate);
    }

    private String normalizeJobStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "OPEN";
        }
        if (!List.of("OPEN", "CLOSED", "DRAFT").contains(normalized)) {
            throw new ResponseStatusException(BAD_REQUEST, "Job status must be one of OPEN, CLOSED, DRAFT");
        }
        return normalized;
    }

    private RecruitmentStage normalizeStage(String stage) {
        if (stage == null || stage.trim().isBlank()) {
            return RecruitmentStage.APPLIED;
        }
        try {
            return RecruitmentStage.valueOf(stage.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid recruitment stage");
        }
    }

    private RecruitmentStage normalizeStageFilter(String stage) {
        if (stage == null || stage.trim().isBlank() || "ALL".equalsIgnoreCase(stage.trim())) {
            return null;
        }
        return normalizeStage(stage);
    }

    private Long resolveOwnerEmployeeId(Employee actor, Long ownerEmployeeId) {
        if (ownerEmployeeId == null) {
            return actor != null ? actor.getId() : null;
        }
        tenantAccessService.assertCanAccessEmployeeId(actor, ownerEmployeeId);
        return ownerEmployeeId;
    }

    private String toNullableTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}

