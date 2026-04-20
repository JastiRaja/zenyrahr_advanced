package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.EmploymentType;
import com.zenyrahr.hrms.service.EmploymentTypeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/EmploymentType")
@RequiredArgsConstructor
public class EmploymentTypeController {

    private final EmploymentTypeDataService employmentTypeDataService;

    @GetMapping
    public ResponseEntity<List<EmploymentType>> getAllEmploymentTypes() {
        return ResponseEntity.ok(employmentTypeDataService.getAllEmploymentTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmploymentType> getEmploymentTypeById(@PathVariable Long id) {
        return employmentTypeDataService.getEmploymentTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmploymentType> createEmploymentType(@RequestBody EmploymentType employmentType) {
        return ResponseEntity.ok(employmentTypeDataService.createEmploymentType(employmentType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentType> updateEmploymentType(@PathVariable Long id, @RequestBody EmploymentType employmentType) {
        return ResponseEntity.ok(employmentTypeDataService.updateEmploymentType(id, employmentType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmploymentType(@PathVariable Long id) {
        employmentTypeDataService.deleteEmploymentType(id);
        return ResponseEntity.noContent().build();
    }
}
