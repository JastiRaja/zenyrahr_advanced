package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.IdentificationDetails;
import com.zenyrahr.hrms.Repository.IdentificationDetailsRepository;
//import com.talvox.hrms.model.PersonalInformation;
import com.zenyrahr.hrms.service.IdentificationDetailsService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdentificationDetailsServiceImpl implements IdentificationDetailsService {

    private final IdentificationDetailsRepository identificationDetailsRepository;
    private final SequenceService sequenceService;

    @Override
    public IdentificationDetails createIdentificationDetails(IdentificationDetails identificationDetails) {
        identificationDetails.setCode(sequenceService.getNextCode("IDDETAILS"));
        return identificationDetailsRepository.save(identificationDetails);
    }

    @Override
    public Optional<IdentificationDetails> getIdentificationDetailsById(Long Id) {
        return identificationDetailsRepository.findById(Id);
    }

    @Override
    public List<IdentificationDetails> getAllIdentificationDetails() {
        return identificationDetailsRepository.findAll();
    }

    @Override
    public IdentificationDetails updateIdentificationDetails(Long Id, IdentificationDetails identificationDetails) {
        if (identificationDetailsRepository.existsById(Id)) {
            identificationDetails.setId(Id);
            return identificationDetailsRepository.save(identificationDetails);
        } else {
            throw new RuntimeException("Identification Details not found");
        }
    }

    @Override
    public void deleteIdentificationDetails(Long Id) {
        identificationDetailsRepository.deleteById(Id);
    }
}
