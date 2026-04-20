package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Department;
import com.zenyrahr.hrms.service.DepartmentDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/Department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentDataService departmentDataService;

    @GetMapping
    public ResponseEntity<List<Department>> getDepartmentData(
            @RequestParam(required = false) Long organizationId
    ) {
        List<Department> all = departmentDataService.getAllDepartmentData();
        if (organizationId != null) {
            all = all.stream()
                    .filter(d -> d.getOrganization() != null && organizationId.equals(d.getOrganization().getId()))
                    .toList();
        }
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentDataById(@PathVariable Long id) {
        return departmentDataService.getDepartmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Department> createDepartmentData(@RequestBody Department department) {
        return ResponseEntity.ok(departmentDataService.createDepartment(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartmentData(@PathVariable Long id, @RequestBody Department department) {
        return ResponseEntity.ok(departmentDataService.updateDepartment(id, department));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartmentData(@PathVariable Long id) {
        departmentDataService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
