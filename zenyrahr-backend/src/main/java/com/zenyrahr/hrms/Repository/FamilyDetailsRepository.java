package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.FamilyDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyDetailsRepository extends JpaRepository<FamilyDetails, Long> {
    Optional<FamilyDetails> findByEmployee_Id(Long employeeId);
}
