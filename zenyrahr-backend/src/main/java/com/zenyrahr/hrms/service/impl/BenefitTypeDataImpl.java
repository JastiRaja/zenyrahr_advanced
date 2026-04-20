package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.BenefitType;
import com.zenyrahr.hrms.Repository.BenefitTypeRepository;
import com.zenyrahr.hrms.service.BenefitTypeDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BenefitTypeDataImpl implements BenefitTypeDataService {

    private final BenefitTypeRepository benefitTypeRepository;
    private final SequenceService sequenceService;

    @Override
    public BenefitType createBenefitType(BenefitType benefitType) {
        benefitType.setCode(sequenceService.getNextCode("BENETYPE"));
        return benefitTypeRepository.save(benefitType);
    }

    @Override
    public Optional<BenefitType> getBenefitTypeById(Long id) {
        return benefitTypeRepository.findById(id);
    }

    @Override
    public List<BenefitType> getAllBenefitTypes() {
        return benefitTypeRepository.findAll();
    }

    @Override
    public BenefitType updateBenefitType(Long id, BenefitType benefitType) {
        if (benefitTypeRepository.existsById(id)) {
            benefitType.setId(id);
            return benefitTypeRepository.save(benefitType);
        } else {
            throw new RuntimeException("BenefitType not found");
        }
    }

    @Override
    public void deleteBenefitType(Long id) {
        benefitTypeRepository.deleteById(id);
    }
}
