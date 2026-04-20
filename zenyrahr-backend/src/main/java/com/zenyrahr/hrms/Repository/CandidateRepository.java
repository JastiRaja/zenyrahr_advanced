package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByOrganization_IdOrderByIdDesc(Long organizationId);
}

