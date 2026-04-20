package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.AdditionalInformation;
import com.zenyrahr.hrms.Repository.AdditionalInformationRepository;
import com.zenyrahr.hrms.service.AdditionalInformationService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdditionalInformationServiceImpl implements AdditionalInformationService {

    private final AdditionalInformationRepository additionalInformationRepository;
    private final SequenceService sequenceService;

    @Override
    public AdditionalInformation createAdditionalInformation(AdditionalInformation additionalInformation) {
        additionalInformation.setCode(sequenceService.getNextCode("ADDINFO"));
        return additionalInformationRepository.save(additionalInformation);
    }

    @Override
    public Optional<AdditionalInformation> getAdditionalInformationById(Long id) {
        return additionalInformationRepository.findById(id);
    }

    @Override
    public List<AdditionalInformation> getAllAdditionalInformation() {
        return additionalInformationRepository.findAll();
    }

    @Override
    public AdditionalInformation updateAdditionalInformation(Long id, AdditionalInformation additionalInformation) {
        if (additionalInformationRepository.existsById(id)) {
            additionalInformation.setId(id);
            return additionalInformationRepository.save(additionalInformation);
        } else {
            throw new RuntimeException("Additional Information not found");
        }
    }

    @Override
    public void deleteAdditionalInformation(Long id) {
        additionalInformationRepository.deleteById(id);
    }
}
