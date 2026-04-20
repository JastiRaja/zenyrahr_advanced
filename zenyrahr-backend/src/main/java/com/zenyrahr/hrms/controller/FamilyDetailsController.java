package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.FamilyDetails;
import com.zenyrahr.hrms.service.FamilyDetailsService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/FamilyDetails")
@RequiredArgsConstructor
public class FamilyDetailsController {

    private final FamilyDetailsService familyDetailsService;
    private final SequenceService sequenceService;


    @PostMapping("/{id}")
    public ResponseEntity<FamilyDetails> createFamilyDetails(@RequestBody FamilyDetails familyDetails) {
        return new ResponseEntity<>(familyDetailsService.createFamilyDetails(familyDetails), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyDetails> getFamilyDetailsById(@PathVariable Long id) {
        Optional<FamilyDetails> familyDetails = familyDetailsService.getFamilyDetailsById(id);
        return familyDetails.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public List<FamilyDetails> getAllFamilyDetails() {
        return familyDetailsService.getAllFamilyDetails();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyDetails> updateFamilyDetails(
            @PathVariable Long id,
            @RequestBody FamilyDetails familyDetails) {
        return new ResponseEntity<>(familyDetailsService.updateFamilyDetails(id, familyDetails), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFamilyDetails(@PathVariable Long id) {
        familyDetailsService.deleteFamilyDetails(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
