package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.SalaryStructure;
import com.zenyrahr.hrms.service.SalaryStructureService;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/salary-structure")
public class SalaryStructureController {
    private final SalaryStructureService salaryStructureService;
    private final TenantAccessService tenantAccessService;

    public SalaryStructureController(
            SalaryStructureService salaryStructureService,
            TenantAccessService tenantAccessService
    ) {
        this.salaryStructureService = salaryStructureService;
        this.tenantAccessService = tenantAccessService;
    }

    private void requireMainAdmin() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (!tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only main admin can manage salary structures");
        }
    }

    @GetMapping
    public ResponseEntity<List<SalaryStructure>> getAllSalaryStructures() {
        return ResponseEntity.ok(salaryStructureService.getAllSalaryStructures());
    }

    @PostMapping
    public ResponseEntity<SalaryStructure> createSalaryStructure(@RequestBody SalaryStructure salaryStructure) {
        requireMainAdmin();
        return ResponseEntity.ok(salaryStructureService.createSalaryStructure(salaryStructure));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryStructure> updateSalaryStructure(@PathVariable Long id, @RequestBody SalaryStructure salaryStructure) {
        requireMainAdmin();
        return ResponseEntity.ok(salaryStructureService.updateSalaryStructure(id, salaryStructure));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalaryStructure(@PathVariable Long id) {
        requireMainAdmin();
        salaryStructureService.deleteSalaryStructure(id);
        return ResponseEntity.noContent().build();
    }
}
