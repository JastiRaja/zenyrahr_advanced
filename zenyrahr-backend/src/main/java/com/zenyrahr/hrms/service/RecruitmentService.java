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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final TenantAccessService tenantAccessService;
    private final OrganizationRepository organizationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateRepository candidateRepository;

    public List<JobPosting> listJobPostings(Employee actor, Long organizationId) {
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return jobPostingRepository.findByOrganization_IdOrderByIdDesc(scopedOrgId);
    }

    public List<Candidate> listCandidates(Employee actor, Long organizationId) {
        Long scopedOrgId = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return candidateRepository.findByOrganization_IdOrderByIdDesc(scopedOrgId);
    }

    public JobPosting createJobPosting(
            Employee actor,
            Long organizationId,
            String title,
            String department,
            String status
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
        posting.setOrganization(organization);
        return jobPostingRepository.save(posting);
    }

    public Candidate createCandidate(
            Employee actor,
            Long organizationId,
            String fullName,
            String email,
            String stage,
            Long jobPostingId
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
        candidate.setJobPosting(jobPosting);
        candidate.setOrganization(organization);
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

    private String toNullableTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}

