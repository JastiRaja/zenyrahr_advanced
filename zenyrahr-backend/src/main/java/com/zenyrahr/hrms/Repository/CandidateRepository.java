package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Candidate;
import com.zenyrahr.hrms.model.RecruitmentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByOrganization_IdOrderByIdDesc(Long organizationId);
    List<Candidate> findByOrganization_IdAndStageOrderByIdDesc(Long organizationId, RecruitmentStage stage);
    Optional<Candidate> findByIdAndOrganization_Id(Long id, Long organizationId);
}

