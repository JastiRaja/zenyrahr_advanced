package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.EducationalBackground;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EducationalBackgroundRepository extends JpaRepository<EducationalBackground, Long> {
    Optional<EducationalBackground> findByEmployee_Id(Long employeeId);
}
