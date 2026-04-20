package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByOrganization_IdOrderByIdDesc(Long organizationId);
    Optional<JobPosting> findByIdAndOrganization_Id(Long id, Long organizationId);
}

