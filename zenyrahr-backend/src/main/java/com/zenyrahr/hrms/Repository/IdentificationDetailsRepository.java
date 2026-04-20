package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.IdentificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentificationDetailsRepository extends JpaRepository<IdentificationDetails, Long> {
    Optional<IdentificationDetails> findByEmployee_Id(Long employeeId);
}
