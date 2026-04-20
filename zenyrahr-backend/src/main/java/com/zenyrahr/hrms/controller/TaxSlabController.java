package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.TaxSlab;
import com.zenyrahr.hrms.service.TaxSlabService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax-slab")
public class TaxSlabController {
    private final TaxSlabService taxSlabService;

    public TaxSlabController(TaxSlabService taxSlabService) {
        this.taxSlabService = taxSlabService;
    }

    @GetMapping
    public ResponseEntity<List<TaxSlab>> getAllTaxSlabs() {
        return ResponseEntity.ok(taxSlabService.getAllTaxSlabs());
    }

    @PostMapping
    public ResponseEntity<TaxSlab> createTaxSlab(@RequestBody TaxSlab taxSlab) {
        return ResponseEntity.ok(taxSlabService.createTaxSlab(taxSlab));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxSlab> updateTaxSlab(@PathVariable Long id, @RequestBody TaxSlab taxSlab) {
        return ResponseEntity.ok(taxSlabService.updateTaxSlab(id, taxSlab));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaxSlab(@PathVariable Long id) {
        taxSlabService.deleteTaxSlab(id);
        return ResponseEntity.noContent().build();
    }
}