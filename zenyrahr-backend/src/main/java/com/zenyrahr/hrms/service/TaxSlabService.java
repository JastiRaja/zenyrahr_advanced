package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.TaxSlab;

import java.util.List;

public interface TaxSlabService {
    List<TaxSlab> getAllTaxSlabs();

    TaxSlab createTaxSlab(TaxSlab taxSlab);

    TaxSlab updateTaxSlab(Long id, TaxSlab taxSlab);

    void deleteTaxSlab(Long id);

}
