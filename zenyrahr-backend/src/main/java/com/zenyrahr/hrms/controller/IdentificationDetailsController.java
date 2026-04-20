package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.IdentificationDetails;
import com.zenyrahr.hrms.service.IdentificationDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/IdentificationDetails")
@RequiredArgsConstructor
public class IdentificationDetailsController {

    private final IdentificationDetailsService identificationDetailsService;

    @GetMapping
    public ResponseEntity<List<IdentificationDetails>> getAllIdentificationDetails() {
        return ResponseEntity.ok(identificationDetailsService.getAllIdentificationDetails());
    }

    @GetMapping("/{Id}")
    public ResponseEntity<IdentificationDetails> getIdentificationDetailsById(@PathVariable Long Id) {
        return identificationDetailsService.getIdentificationDetailsById(Id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<IdentificationDetails> createIdentificationDetails(@RequestBody IdentificationDetails identificationDetails) {
        return ResponseEntity.ok(identificationDetailsService.createIdentificationDetails(identificationDetails));
    }

    @PutMapping("/{Id}")
    public ResponseEntity<IdentificationDetails> updateIdentificationDetails(@PathVariable Long Id,
                                                                             @RequestBody IdentificationDetails identificationDetails) {
        return ResponseEntity.ok(identificationDetailsService.updateIdentificationDetails(Id, identificationDetails));
    }

    @DeleteMapping("/{Id}")
    public ResponseEntity<Void> deleteIdentificationDetails(@PathVariable Long Id) {
        identificationDetailsService.deleteIdentificationDetails(Id);
        return ResponseEntity.noContent().build();
    }
}
