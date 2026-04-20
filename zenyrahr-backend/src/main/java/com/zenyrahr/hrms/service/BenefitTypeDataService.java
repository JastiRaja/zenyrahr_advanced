package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.BenefitType;

import java.util.List;
import java.util.Optional;

public interface BenefitTypeDataService {

    BenefitType createBenefitType(BenefitType benefitType);

    Optional<BenefitType> getBenefitTypeById(Long id);

    List<BenefitType> getAllBenefitTypes();

    BenefitType updateBenefitType(Long id, BenefitType benefitType);

    void deleteBenefitType(Long id);
}
