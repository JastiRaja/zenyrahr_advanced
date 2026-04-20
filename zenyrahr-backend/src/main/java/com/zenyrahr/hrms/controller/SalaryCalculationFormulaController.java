package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.SalaryCalculationFormula;
import com.zenyrahr.hrms.service.SalaryCalculationFormulaService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-formula")
@RequiredArgsConstructor
public class SalaryCalculationFormulaController {

    private final SalaryCalculationFormulaService formulaService;
    private final TenantAccessService tenantAccessService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<List<SalaryCalculationFormula>> getFormulas(@RequestParam Long organizationId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(formulaService.getFormulas(scopedOrg));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<SalaryCalculationFormula> createFormula(
            @RequestParam Long organizationId,
            @RequestBody SalaryCalculationFormula formula) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(formulaService.createFormula(scopedOrg, formula));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<SalaryCalculationFormula> updateFormula(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @RequestBody SalaryCalculationFormula formula) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(formulaService.updateFormula(scopedOrg, id, formula));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<Void> deleteFormula(@PathVariable Long id, @RequestParam Long organizationId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        formulaService.deleteFormula(scopedOrg, id);
        return ResponseEntity.noContent().build();
    }
}
