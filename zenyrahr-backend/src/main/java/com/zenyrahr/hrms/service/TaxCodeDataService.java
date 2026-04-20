package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.TaxCode;

import java.util.List;
import java.util.Optional;

public interface TaxCodeDataService {

    TaxCode createTaxCode(TaxCode taxCode);

    Optional<TaxCode> getTaxCodeById(Long id);

    List<TaxCode> getAllTaxCodes();

    TaxCode updateTaxCode(Long id, TaxCode taxCode);

    void deleteTaxCode(Long id);
}
