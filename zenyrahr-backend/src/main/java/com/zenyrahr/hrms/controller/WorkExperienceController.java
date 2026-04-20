package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.WorkExperience;
import com.zenyrahr.hrms.service.WorkExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-experience")
@RequiredArgsConstructor
public class WorkExperienceController {

    private final WorkExperienceService workExperienceService;

    @PostMapping
    public ResponseEntity<WorkExperience> createWorkExperience(@RequestBody WorkExperience workExperience) {
        return ResponseEntity.ok(workExperienceService.createWorkExperience(workExperience)); // Create work experience record
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkExperience> getWorkExperienceById(@PathVariable Long id) {
        return workExperienceService.getWorkExperienceById(id)
                .map(ResponseEntity::ok) // Fetch work experience by id
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WorkExperience>> getAllWorkExperience() {
        return ResponseEntity.ok(workExperienceService.getAllWorkExperience()); // Fetch all work experience records
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkExperience> updateWorkExperience(@PathVariable Long id, @RequestBody WorkExperience workExperience) {
        return ResponseEntity.ok(workExperienceService.updateWorkExperience(id, workExperience)); // Update work experience record
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkExperience(@PathVariable Long id) {
        workExperienceService.deleteWorkExperience(id); // Delete work experience by id
        return ResponseEntity.noContent().build();
    }
}
