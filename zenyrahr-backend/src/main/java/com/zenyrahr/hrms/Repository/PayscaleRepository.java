package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Payscale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayscaleRepository extends JpaRepository<Payscale, Long> {
    Optional<Payscale> findByEmployeeIdAndStatus(Long employeeId, String status);
    List<Payscale> findByStatus(String status);
    List<Payscale> findByEmployeeId(Long employeeId);

    List<Payscale> findByEmployee_Organization_IdOrderByIdDesc(Long organizationId);
} 