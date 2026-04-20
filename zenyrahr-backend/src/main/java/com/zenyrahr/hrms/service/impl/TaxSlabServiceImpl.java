package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.TaxSlabRepository;
import com.zenyrahr.hrms.model.TaxSlab;
import com.zenyrahr.hrms.service.TaxSlabService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class TaxSlabServiceImpl implements TaxSlabService {

    private final TaxSlabRepository taxSlabRepository;

    public TaxSlabServiceImpl(TaxSlabRepository taxSlabRepository) {
        this.taxSlabRepository = taxSlabRepository;
    }

    @Override
    public List<TaxSlab> getAllTaxSlabs() {
        return taxSlabRepository.findAll();
    }

    @Override
    public TaxSlab createTaxSlab(TaxSlab taxSlab) {
        return taxSlabRepository.save(taxSlab);
    }

    @Override
    public TaxSlab updateTaxSlab(Long id, TaxSlab taxSlab) {
        taxSlab.setId(id);
        return taxSlabRepository.save(taxSlab);
    }

    @Override
    public void deleteTaxSlab(Long id) {
        taxSlabRepository.deleteById(id);
    }
}