package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.FamilyDetails;
import com.zenyrahr.hrms.Repository.FamilyDetailsRepository;
import com.zenyrahr.hrms.service.FamilyDetailsService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FamilyDetailsServiceImpl implements FamilyDetailsService {

    private final FamilyDetailsRepository familyDetailsRepository;
    private final SequenceService sequenceService;

    @Override
    public FamilyDetails createFamilyDetails(FamilyDetails familyDetails) {
//        familyDetails.setCode(sequenceService.getNextCode("FAMDETAILS"));
        return familyDetailsRepository.save(familyDetails);
    }

    @Override
    public Optional<FamilyDetails> getFamilyDetailsById(Long id) {
        return familyDetailsRepository.findById(id);
    }

    @Override
    public List<FamilyDetails> getAllFamilyDetails() {
        return familyDetailsRepository.findAll();
    }

    @Override
    public FamilyDetails updateFamilyDetails(Long id, FamilyDetails familyDetails) {
//        familyDetails.setCode(sequenceService.getNextCode("FAMDETAILS"));
        if (familyDetailsRepository.existsById(id)) {
            familyDetails.setId(id);  // Set existing 'id' for update
            return familyDetailsRepository.save(familyDetails);
        } else {
            throw new RuntimeException("Family Details not found");
        }
    }

    @Override
    public void deleteFamilyDetails(Long id) {
        familyDetailsRepository.deleteById(id);
    }
}
