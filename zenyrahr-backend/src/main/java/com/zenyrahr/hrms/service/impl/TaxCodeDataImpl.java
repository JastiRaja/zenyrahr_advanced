package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.TaxCode;
import com.zenyrahr.hrms.Repository.TaxCodeRepository;
import com.zenyrahr.hrms.service.TaxCodeDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaxCodeDataImpl implements TaxCodeDataService {

    private final TaxCodeRepository taxCodeRepository;
    private final SequenceService sequenceService;

    @Override
    public TaxCode createTaxCode(TaxCode taxCode) {
        taxCode.setCode(sequenceService.getNextCode("TAXCODE"));
        return taxCodeRepository.save(taxCode);
    }

    @Override
    public Optional<TaxCode> getTaxCodeById(Long id) {
        return taxCodeRepository.findById(id);
    }

    @Override
    public List<TaxCode> getAllTaxCodes() {
        return taxCodeRepository.findAll();
    }

    @Override
    public TaxCode updateTaxCode(Long id, TaxCode taxCode) {
        if (taxCodeRepository.existsById(id)) {
            taxCode.setId(id);
            return taxCodeRepository.save(taxCode);
        } else {
            throw new RuntimeException("TaxCode not found");
        }
    }

    @Override
    public void deleteTaxCode(Long id) {
        taxCodeRepository.deleteById(id);
    }
}
