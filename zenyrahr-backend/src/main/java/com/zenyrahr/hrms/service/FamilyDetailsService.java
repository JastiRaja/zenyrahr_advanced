package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.FamilyDetails;

import java.util.List;
import java.util.Optional;

public interface FamilyDetailsService {

    FamilyDetails createFamilyDetails(FamilyDetails familyDetails);

    Optional<FamilyDetails> getFamilyDetailsById(Long id);

    List<FamilyDetails> getAllFamilyDetails();

    FamilyDetails updateFamilyDetails(Long id, FamilyDetails familyDetails);

    void deleteFamilyDetails(Long id);
}
