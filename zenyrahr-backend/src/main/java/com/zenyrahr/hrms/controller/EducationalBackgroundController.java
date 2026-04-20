package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.EducationalBackground;
import com.zenyrahr.hrms.service.EducationalBackgroundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/educational-background")
@RequiredArgsConstructor
public class EducationalBackgroundController {

    private final EducationalBackgroundService educationalBackgroundService;

    @GetMapping
    public ResponseEntity<List<EducationalBackground>> getAllEducationalBackgrounds() {
        return ResponseEntity.ok(educationalBackgroundService.getAllEducationalBackgrounds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EducationalBackground> getEducationalBackgroundById(@PathVariable Long id) {
        return educationalBackgroundService.getEducationalBackgroundById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EducationalBackground> createEducationalBackground(@RequestBody EducationalBackground educationalBackground) {
        return ResponseEntity.ok(educationalBackgroundService.createEducationalBackground(educationalBackground));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EducationalBackground> updateEducationalBackground(
            @PathVariable Long id,
            @RequestBody EducationalBackground educationalBackground) {
        return ResponseEntity.ok(educationalBackgroundService.updateEducationalBackground(id, educationalBackground));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEducationalBackground(@PathVariable Long id) {
        educationalBackgroundService.deleteEducationalBackground(id);
        return ResponseEntity.noContent().build();
    }
}
