package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.SalaryAndBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryAndBankDetailsRepository extends JpaRepository<SalaryAndBankDetails, Long> {
    Optional<SalaryAndBankDetails> findByEmployee_Id(Long employeeId);
}
