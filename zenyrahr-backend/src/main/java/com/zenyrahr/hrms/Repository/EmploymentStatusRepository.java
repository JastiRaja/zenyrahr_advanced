package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentStatusRepository extends JpaRepository<EmploymentStatus, Long> {
}
