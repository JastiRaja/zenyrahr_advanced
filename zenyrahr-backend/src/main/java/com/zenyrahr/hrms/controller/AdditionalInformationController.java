package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.AdditionalInformation;
import com.zenyrahr.hrms.service.AdditionalInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/additional-information")
@RequiredArgsConstructor
public class AdditionalInformationController {

    private final AdditionalInformationService additionalInformationService;

    @GetMapping
    public ResponseEntity<List<AdditionalInformation>> getAllAdditionalInformation() {
        return ResponseEntity.ok(additionalInformationService.getAllAdditionalInformation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdditionalInformation> getAdditionalInformationById(@PathVariable Long id) {
        return additionalInformationService.getAdditionalInformationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AdditionalInformation> createAdditionalInformation(@RequestBody AdditionalInformation additionalInformation) {
        return ResponseEntity.ok(additionalInformationService.createAdditionalInformation(additionalInformation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdditionalInformation> updateAdditionalInformation(@PathVariable Long id, @RequestBody AdditionalInformation additionalInformation) {
        return ResponseEntity.ok(additionalInformationService.updateAdditionalInformation(id, additionalInformation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdditionalInformation(@PathVariable Long id) {
        additionalInformationService.deleteAdditionalInformation(id);
        return ResponseEntity.noContent().build();
    }
}
