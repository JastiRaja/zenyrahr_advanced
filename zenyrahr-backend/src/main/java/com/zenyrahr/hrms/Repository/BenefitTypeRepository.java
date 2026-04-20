package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.BenefitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitTypeRepository extends JpaRepository<BenefitType, Long> {
}
