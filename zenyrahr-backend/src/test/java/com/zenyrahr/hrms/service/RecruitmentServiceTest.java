package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.CandidateRepository;
import com.zenyrahr.hrms.Repository.JobPostingRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.model.Candidate;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.model.RecruitmentStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Test
    void transitionCandidateStageRejectsInvalidTransition() {
        Employee actor = actor(7L, "hr");
        Candidate candidate = new Candidate();
        candidate.setId(50L);
        candidate.setStage(RecruitmentStage.APPLIED);

        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 1L)).thenReturn(1L);
        when(candidateRepository.findByIdAndOrganization_Id(50L, 1L)).thenReturn(Optional.of(candidate));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recruitmentService.transitionCandidateStage(actor, 1L, 50L, "HIRED", null, null)
        );

        assertEquals(BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid stage transition from APPLIED to HIRED", ex.getReason());
    }

    @Test
    void transitionCandidateStageRequiresRejectionReason() {
        Employee actor = actor(7L, "hr");
        Candidate candidate = new Candidate();
        candidate.setId(51L);
        candidate.setStage(RecruitmentStage.INTERVIEW);

        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 2L)).thenReturn(2L);
        when(candidateRepository.findByIdAndOrganization_Id(51L, 2L)).thenReturn(Optional.of(candidate));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recruitmentService.transitionCandidateStage(actor, 2L, 51L, "REJECTED", "not fit", " ")
        );

        assertEquals(BAD_REQUEST, ex.getStatusCode());
        assertEquals("Rejection reason is required when moving to REJECTED", ex.getReason());
    }

    @Test
    void transitionCandidateStageUpdatesCandidateForValidTransition() {
        Employee actor = actor(8L, "org_admin");
        Candidate candidate = new Candidate();
        candidate.setId(52L);
        candidate.setStage(RecruitmentStage.SHORTLISTED);

        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 3L)).thenReturn(3L);
        when(candidateRepository.findByIdAndOrganization_Id(52L, 3L)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Candidate saved = recruitmentService.transitionCandidateStage(
                actor,
                3L,
                52L,
                "INTERVIEW",
                "Strong screening feedback",
                null
        );

        assertEquals(RecruitmentStage.INTERVIEW, saved.getStage());
        assertEquals("Strong screening feedback", saved.getNotes());
        assertNull(saved.getRejectionReason());
        assertNotNull(saved.getStageUpdatedAt());
        verify(candidateRepository).save(candidate);
    }

    @Test
    void transitionCandidateStageUsesResolvedOrganizationScope() {
        Employee actor = actor(9L, "hr");
        Candidate candidate = new Candidate();
        candidate.setId(53L);
        candidate.setStage(RecruitmentStage.OFFERED);

        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, null)).thenReturn(88L);
        when(candidateRepository.findByIdAndOrganization_Id(53L, 88L)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recruitmentService.transitionCandidateStage(actor, null, 53L, "HIRED", null, null);

        verify(candidateRepository).findByIdAndOrganization_Id(53L, 88L);
    }

    @Test
    void createCandidateReturnsNotFoundWhenOrganizationMissing() {
        Employee actor = actor(11L, "hr");
        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 99L)).thenReturn(99L);
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recruitmentService.createCandidate(
                        actor,
                        99L,
                        "Jane Doe",
                        "jane@acme.com",
                        "APPLIED",
                        null,
                        "LinkedIn",
                        "Initial profile",
                        null
                )
        );

        assertEquals(NOT_FOUND, ex.getStatusCode());
        assertEquals("Organization not found", ex.getReason());
    }

    @Test
    void listCandidatesUsesStageFilterWhenProvided() {
        Employee actor = actor(12L, "hr");
        Candidate candidate = new Candidate();
        candidate.setId(61L);
        candidate.setStage(RecruitmentStage.INTERVIEW);

        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 41L)).thenReturn(41L);
        when(candidateRepository.findByOrganization_IdAndStageOrderByIdDesc(41L, RecruitmentStage.INTERVIEW))
                .thenReturn(List.of(candidate));

        List<Candidate> rows = recruitmentService.listCandidates(actor, 41L, "INTERVIEW");

        assertEquals(1, rows.size());
        verify(candidateRepository).findByOrganization_IdAndStageOrderByIdDesc(41L, RecruitmentStage.INTERVIEW);
        verify(candidateRepository, never()).findByOrganization_IdOrderByIdDesc(anyLong());
    }

    @Test
    void listCandidatesRejectsInvalidStageFilter() {
        Employee actor = actor(13L, "hr");
        when(tenantAccessService.resolveOrganizationIdForScopedQuery(actor, 42L)).thenReturn(42L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> recruitmentService.listCandidates(actor, 42L, "NOT_A_STAGE")
        );

        assertEquals(BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid recruitment stage", ex.getReason());
    }

    private Employee actor(Long id, String role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setRole(role);
        Organization organization = new Organization();
        organization.setId(1L);
        employee.setOrganization(organization);
        return employee;
    }
}
