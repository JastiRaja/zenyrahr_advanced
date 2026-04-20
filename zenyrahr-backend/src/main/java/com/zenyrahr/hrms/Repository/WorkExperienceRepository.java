package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {
    Optional<WorkExperience> findByEmployee_Id(Long employeeId);
    // JpaRepository automatically uses 'id' as the primary key by default
}
