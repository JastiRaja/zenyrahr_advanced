package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.HealthAndMedicalInformation;
import com.zenyrahr.hrms.service.HealthAndMedicalInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-medical-info")
@RequiredArgsConstructor
public class HealthAndMedicalInformationController {

    private final HealthAndMedicalInformationService healthAndMedicalInformationService;

    @GetMapping
    public ResponseEntity<List<HealthAndMedicalInformation>> getAllHealthAndMedicalInformation() {
        return ResponseEntity.ok(healthAndMedicalInformationService.getAllHealthAndMedicalInformation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthAndMedicalInformation> getHealthAndMedicalInformationById(@PathVariable Long id) {
        return healthAndMedicalInformationService.getHealthAndMedicalInformationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HealthAndMedicalInformation> createHealthAndMedicalInformation(@RequestBody HealthAndMedicalInformation healthAndMedicalInformation) {
        return ResponseEntity.ok(healthAndMedicalInformationService.createHealthAndMedicalInformation(healthAndMedicalInformation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthAndMedicalInformation> updateHealthAndMedicalInformation(@PathVariable Long id, @RequestBody HealthAndMedicalInformation healthAndMedicalInformation) {
        return ResponseEntity.ok(healthAndMedicalInformationService.updateHealthAndMedicalInformation(id, healthAndMedicalInformation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHealthAndMedicalInformation(@PathVariable Long id) {
        healthAndMedicalInformationService.deleteHealthAndMedicalInformation(id);
        return ResponseEntity.noContent().build();
    }
}
