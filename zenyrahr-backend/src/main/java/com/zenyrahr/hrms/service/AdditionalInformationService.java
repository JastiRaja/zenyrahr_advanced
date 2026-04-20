package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.AdditionalInformation;

import java.util.List;
import java.util.Optional;

public interface AdditionalInformationService {

    AdditionalInformation createAdditionalInformation(AdditionalInformation additionalInformation);

    Optional<AdditionalInformation> getAdditionalInformationById(Long id);

    List<AdditionalInformation> getAllAdditionalInformation();

    AdditionalInformation updateAdditionalInformation(Long id, AdditionalInformation additionalInformation);

    void deleteAdditionalInformation(Long id);
}
