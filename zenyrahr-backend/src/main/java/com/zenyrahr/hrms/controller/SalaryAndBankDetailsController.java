package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.SalaryAndBankDetails;
import com.zenyrahr.hrms.service.SalaryAndBankDetailsService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/SalaryAndBankDetails")
@RequiredArgsConstructor
public class SalaryAndBankDetailsController {

    private final SalaryAndBankDetailsService salaryAndBankDetailsService;
    private final TenantAccessService tenantAccessService;

    @GetMapping
    public ResponseEntity<List<SalaryAndBankDetails>> getAllSalaryAndBankDetails() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<SalaryAndBankDetails> items = salaryAndBankDetailsService.getAllSalaryAndBankDetails();
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long orgId = tenantAccessService.requireOrganizationId(actor);
            items = items.stream()
                    .filter(item -> item.getEmployee() != null
                            && item.getEmployee().getOrganization() != null
                            && orgId.equals(item.getEmployee().getOrganization().getId()))
                    .toList();
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryAndBankDetails> getSalaryAndBankDetailsById(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return salaryAndBankDetailsService.getSalaryAndBankDetailsById(id)
                .map(item -> {
                    tenantAccessService.assertCanAccessEmployee(actor, item.getEmployee());
                    return ResponseEntity.ok(item);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SalaryAndBankDetails> createSalaryAndBankDetails(@RequestBody SalaryAndBankDetails salaryAndBankDetails) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, salaryAndBankDetails.getEmployee().getId());
        return ResponseEntity.ok(salaryAndBankDetailsService.createSalaryAndBankDetails(salaryAndBankDetails));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryAndBankDetails> updateSalaryAndBankDetails(@PathVariable Long id, @RequestBody SalaryAndBankDetails salaryAndBankDetails) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, salaryAndBankDetails.getEmployee().getId());
        return ResponseEntity.ok(salaryAndBankDetailsService.updateSalaryAndBankDetails(id, salaryAndBankDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalaryAndBankDetails(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        SalaryAndBankDetails item = salaryAndBankDetailsService.getSalaryAndBankDetailsById(id)
                .orElseThrow(() -> new RuntimeException("Salary and bank details not found"));
        tenantAccessService.assertCanAccessEmployee(actor, item.getEmployee());
        salaryAndBankDetailsService.deleteSalaryAndBankDetails(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<SalaryAndBankDetails> getByEmployeeId(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return salaryAndBankDetailsService.getAllSalaryAndBankDetails().stream()
            .filter(bd -> bd.getEmployee() != null && bd.getEmployee().getId().equals(employeeId))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
