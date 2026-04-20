package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.TaxCode;
import com.zenyrahr.hrms.service.TaxCodeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/TaxCode")
@RequiredArgsConstructor
public class TaxCodeController {

    private final TaxCodeDataService taxCodeDataService;

    @GetMapping
    public ResponseEntity<List<TaxCode>> getAllTaxCodes() {
        return ResponseEntity.ok(taxCodeDataService.getAllTaxCodes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxCode> getTaxCodeById(@PathVariable Long id) {
        return taxCodeDataService.getTaxCodeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaxCode> createTaxCode(@RequestBody TaxCode taxCode) {
        return ResponseEntity.ok(taxCodeDataService.createTaxCode(taxCode));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxCode> updateTaxCode(@PathVariable Long id, @RequestBody TaxCode taxCode) {
        return ResponseEntity.ok(taxCodeDataService.updateTaxCode(id, taxCode));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaxCode(@PathVariable Long id) {
        taxCodeDataService.deleteTaxCode(id);
        return ResponseEntity.noContent().build();
    }
}
