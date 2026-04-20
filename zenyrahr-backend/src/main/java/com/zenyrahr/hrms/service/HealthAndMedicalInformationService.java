package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.HealthAndMedicalInformation;

import java.util.List;
import java.util.Optional;

public interface HealthAndMedicalInformationService {

    HealthAndMedicalInformation createHealthAndMedicalInformation(HealthAndMedicalInformation healthAndMedicalInformation);

    Optional<HealthAndMedicalInformation> getHealthAndMedicalInformationById(Long id);

    List<HealthAndMedicalInformation> getAllHealthAndMedicalInformation();

    HealthAndMedicalInformation updateHealthAndMedicalInformation(Long id, HealthAndMedicalInformation healthAndMedicalInformation);

    void deleteHealthAndMedicalInformation(Long id);
}
