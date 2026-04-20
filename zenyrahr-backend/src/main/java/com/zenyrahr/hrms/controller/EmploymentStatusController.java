package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.EmploymentStatus;
import com.zenyrahr.hrms.service.EmploymentStatusDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/EmploymentStatus")
@RequiredArgsConstructor
public class EmploymentStatusController {

    private final EmploymentStatusDataService employmentStatusDataService;

    @GetMapping
    public ResponseEntity<List<EmploymentStatus>> getAllEmploymentStatuses() {
        return ResponseEntity.ok(employmentStatusDataService.getAllEmploymentStatuses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmploymentStatus> getEmploymentStatusById(@PathVariable Long id) {
        return employmentStatusDataService.getEmploymentStatusById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmploymentStatus> createEmploymentStatus(@RequestBody EmploymentStatus employmentStatus) {
        return ResponseEntity.ok(employmentStatusDataService.createEmploymentStatus(employmentStatus));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentStatus> updateEmploymentStatus(@PathVariable Long id, @RequestBody EmploymentStatus employmentStatus) {
        return ResponseEntity.ok(employmentStatusDataService.updateEmploymentStatus(id, employmentStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmploymentStatus(@PathVariable Long id) {
        employmentStatusDataService.deleteEmploymentStatus(id);
        return ResponseEntity.noContent().build();
    }
}
