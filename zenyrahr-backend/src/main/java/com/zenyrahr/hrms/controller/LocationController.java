package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Location;
import com.zenyrahr.hrms.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody Location user) {
        return ResponseEntity.ok(locationService.createLocation(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations(
            @RequestParam(required = false) Long organizationId
    ) {
        List<Location> all = locationService.getAllLocation();
        if (organizationId != null) {
            all = all.stream()
                    .filter(d -> d.getOrganization() != null && organizationId.equals(d.getOrganization().getId()))
                    .toList();
        }
        return ResponseEntity.ok(all);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(@PathVariable Long id, @RequestBody Location location) {
        return ResponseEntity.ok(locationService.updateLocation(id, location));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
