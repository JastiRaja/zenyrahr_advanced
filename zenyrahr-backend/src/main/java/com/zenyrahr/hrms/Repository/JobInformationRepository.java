package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.JobInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobInformationRepository extends JpaRepository<JobInformation, Long> {
    Optional<JobInformation> findByEmployee_Id(Long employeeId);
}
