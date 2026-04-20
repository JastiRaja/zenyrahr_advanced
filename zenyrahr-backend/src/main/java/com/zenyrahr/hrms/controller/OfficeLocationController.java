package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.OfficeLocation;
import com.zenyrahr.hrms.service.OfficeLocationDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/OfficeLocation")
@RequiredArgsConstructor
public class OfficeLocationController {

    private final OfficeLocationDataService officeLocationDataService;

    @GetMapping
    public ResponseEntity<List<OfficeLocation>> getAllOfficeLocations() {
        return ResponseEntity.ok(officeLocationDataService.getAllOfficeLocations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficeLocation> getOfficeLocationById(@PathVariable Long id) {
        return officeLocationDataService.getOfficeLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OfficeLocation> createOfficeLocation(@RequestBody OfficeLocation officeLocation) {
        return ResponseEntity.ok(officeLocationDataService.createOfficeLocation(officeLocation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfficeLocation> updateOfficeLocation(@PathVariable Long id, @RequestBody OfficeLocation officeLocation) {
        return ResponseEntity.ok(officeLocationDataService.updateOfficeLocation(id, officeLocation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOfficeLocation(@PathVariable Long id) {
        officeLocationDataService.deleteOfficeLocation(id);
        return ResponseEntity.noContent().build();
    }
}
