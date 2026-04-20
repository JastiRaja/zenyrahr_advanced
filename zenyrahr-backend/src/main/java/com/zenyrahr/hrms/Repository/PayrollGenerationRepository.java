package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.PayrollGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollGenerationRepository extends JpaRepository<PayrollGeneration, Long> {
    Optional<PayrollGeneration> findFirstByOrganizationIdAndMonthYear(Long organizationId, String monthYear);

    List<PayrollGeneration> findByOrganizationIdOrderByGenerationDateDesc(Long organizationId);

    List<PayrollGeneration> findByOrganizationIdAndStatus(Long organizationId, String status);

    List<PayrollGeneration> findAllByOrganizationIdAndMonthYear(Long organizationId, String monthYear);
} 