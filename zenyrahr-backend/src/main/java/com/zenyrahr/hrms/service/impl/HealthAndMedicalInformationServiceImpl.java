package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.HealthAndMedicalInformation;
import com.zenyrahr.hrms.Repository.HealthAndMedicalInformationRepository;
import com.zenyrahr.hrms.service.HealthAndMedicalInformationService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthAndMedicalInformationServiceImpl implements HealthAndMedicalInformationService {

    private final HealthAndMedicalInformationRepository healthAndMedicalInformationRepository;
    private final SequenceService sequenceService;

    @Override
    public HealthAndMedicalInformation createHealthAndMedicalInformation(HealthAndMedicalInformation healthAndMedicalInformation) {
        healthAndMedicalInformation.setCode(sequenceService.getNextCode("HEALTHMEDINFO"));
        return healthAndMedicalInformationRepository.save(healthAndMedicalInformation);
    }

    @Override
    public Optional<HealthAndMedicalInformation> getHealthAndMedicalInformationById(Long id) {
        return healthAndMedicalInformationRepository.findById(id);
    }

    @Override
    public List<HealthAndMedicalInformation> getAllHealthAndMedicalInformation() {
        return healthAndMedicalInformationRepository.findAll();
    }

    @Override
    public HealthAndMedicalInformation updateHealthAndMedicalInformation(Long id, HealthAndMedicalInformation healthAndMedicalInformation) {
        if (healthAndMedicalInformationRepository.existsById(id)) {
            healthAndMedicalInformation.setId(id);
            return healthAndMedicalInformationRepository.save(healthAndMedicalInformation);
        } else {
            throw new RuntimeException("Health and Medical Information not found");
        }
    }

    @Override
    public void deleteHealthAndMedicalInformation(Long id) {
        healthAndMedicalInformationRepository.deleteById(id);
    }
}
