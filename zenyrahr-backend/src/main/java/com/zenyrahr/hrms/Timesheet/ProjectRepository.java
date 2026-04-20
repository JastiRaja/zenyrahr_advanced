package com.zenyrahr.hrms.Timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOrganization_Id(Long organizationId);
    Optional<Project> findByIdAndOrganization_Id(Long id, Long organizationId);
    long countByOrganization_IdAndStatusIgnoreCase(Long organizationId, String status);
}
