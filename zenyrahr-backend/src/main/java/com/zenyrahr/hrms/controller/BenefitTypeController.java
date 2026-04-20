package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.BenefitType;
import com.zenyrahr.hrms.service.BenefitTypeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/BenefitType")
@RequiredArgsConstructor
public class BenefitTypeController {

    private final BenefitTypeDataService benefitTypeDataService;

    @GetMapping
    public ResponseEntity<List<BenefitType>> getAllBenefitTypes() {
        return ResponseEntity.ok(benefitTypeDataService.getAllBenefitTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BenefitType> getBenefitTypeById(@PathVariable Long id) {
        return benefitTypeDataService.getBenefitTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BenefitType> createBenefitType(@RequestBody BenefitType benefitType) {
        return ResponseEntity.ok(benefitTypeDataService.createBenefitType(benefitType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BenefitType> updateBenefitType(@PathVariable Long id, @RequestBody BenefitType benefitType) {
        return ResponseEntity.ok(benefitTypeDataService.updateBenefitType(id, benefitType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBenefitType(@PathVariable Long id) {
        benefitTypeDataService.deleteBenefitType(id);
        return ResponseEntity.noContent().build();
    }
}
