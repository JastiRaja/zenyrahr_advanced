package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.IdentificationDetails;

import java.util.List;
import java.util.Optional;

public interface IdentificationDetailsService {

    IdentificationDetails createIdentificationDetails(IdentificationDetails identificationDetails);

    Optional<IdentificationDetails> getIdentificationDetailsById(Long Id);

    List<IdentificationDetails> getAllIdentificationDetails();

    IdentificationDetails updateIdentificationDetails(Long Id, IdentificationDetails identificationDetails);

    void deleteIdentificationDetails(Long Id);
}
