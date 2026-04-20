package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.HealthAndMedicalInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthAndMedicalInformationRepository extends JpaRepository<HealthAndMedicalInformation, Long> {
    Optional<HealthAndMedicalInformation> findByEmployee_Id(Long employeeId);
}
