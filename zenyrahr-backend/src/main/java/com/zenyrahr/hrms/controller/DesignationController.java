package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Designation;
import com.zenyrahr.hrms.service.DesignationDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/Designation")
@RequiredArgsConstructor
public class DesignationController {

    private final DesignationDataService designationDataService;

    @GetMapping
    public ResponseEntity<List<Designation>> getAllDesignations(
            @RequestParam(required = false) Long organizationId
    ) {
        List<Designation> all = designationDataService.getAllDesignations();
        if (organizationId != null) {
            all = all.stream()
                    .filter(d -> d.getOrganization() != null && organizationId.equals(d.getOrganization().getId()))
                    .toList();
        }
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Designation> getDesignationById(@PathVariable Long id) {
        return designationDataService.getDesignationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Designation> createDesignation(@RequestBody Designation designation) {
        return ResponseEntity.ok(designationDataService.createDesignation(designation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Designation> updateDesignation(@PathVariable Long id, @RequestBody Designation designation) {
        return ResponseEntity.ok(designationDataService.updateDesignation(id, designation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesignation(@PathVariable Long id) {
        designationDataService.deleteDesignation(id);
        return ResponseEntity.noContent().build();
    }
}
