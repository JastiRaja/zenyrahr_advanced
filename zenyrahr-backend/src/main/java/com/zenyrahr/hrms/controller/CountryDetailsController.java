package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.CountryDetails;
import com.zenyrahr.hrms.service.CountryDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/country-details")
@RequiredArgsConstructor
public class CountryDetailsController {

    private final CountryDetailsService countryDetailsService;

    @PostMapping
    public ResponseEntity<CountryDetails> createCountryDetails(@RequestBody CountryDetails countryDetails) {
        return ResponseEntity.ok(countryDetailsService.createCountryDetails(countryDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryDetails> getCountryDetailsById(@PathVariable Long id) {
        return countryDetailsService.getCountryDetailsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<CountryDetails>> getAllCountryDetails() {
        return ResponseEntity.ok(countryDetailsService.getAllCountryDetails());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryDetails> updateCountryDetails(@PathVariable Long id, @RequestBody CountryDetails countryDetails) {
        try {
            return ResponseEntity.ok(countryDetailsService.updateCountryDetails(id, countryDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountryDetails(@PathVariable Long id) {
        countryDetailsService.deleteCountryDetails(id);
        return ResponseEntity.noContent().build();
    }
}
