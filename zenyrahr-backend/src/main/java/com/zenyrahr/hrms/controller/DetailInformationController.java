package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.DetailInformation;
import com.zenyrahr.hrms.service.DetailInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detail-information")
@RequiredArgsConstructor
public class DetailInformationController {

    private final DetailInformationService detailInformationService;

    @PostMapping
    public ResponseEntity<DetailInformation> createDetailInformation(@RequestBody DetailInformation detailInformation) {
        return ResponseEntity.ok(detailInformationService.createDetailInformation(detailInformation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailInformation> getDetailInformationById(@PathVariable Long id) {
        return detailInformationService.getDetailInformationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DetailInformation>> getAllDetailInformation() {
        return ResponseEntity.ok(detailInformationService.getAllDetailInformation());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetailInformation> updateDetailInformation(@PathVariable Long id, @RequestBody DetailInformation detailInformation) {
        return ResponseEntity.ok(detailInformationService.updateDetailInformation(id, detailInformation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDetailInformation(@PathVariable Long id) {
        detailInformationService.deleteDetailInformation(id);
        return ResponseEntity.noContent().build();
    }
}