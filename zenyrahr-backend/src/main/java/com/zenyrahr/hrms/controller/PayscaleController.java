package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Payscale;
import com.zenyrahr.hrms.service.PayscaleService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payscale")
@RequiredArgsConstructor
public class PayscaleController {

    private final PayscaleService payscaleService;
    private final TenantAccessService tenantAccessService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payscale> createPayscale(@RequestBody Payscale payscale) {
        tenantAccessService.assertCanAccessEmployeeId(
                tenantAccessService.requireCurrentEmployee(),
                payscale.getEmployee().getId()
        );
        return ResponseEntity.ok(payscaleService.createPayscale(payscale));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payscale> updatePayscale(@PathVariable Long id, @RequestBody Payscale payscale) {
        tenantAccessService.assertCanAccessEmployeeId(
                tenantAccessService.requireCurrentEmployee(),
                payscale.getEmployee().getId()
        );
        return ResponseEntity.ok(payscaleService.updatePayscale(id, payscale));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Void> deletePayscale(@PathVariable Long id) {
        var actor = tenantAccessService.requireCurrentEmployee();
        var existing = payscaleService.getPayscaleById(id);
        tenantAccessService.assertCanAccessEmployeeId(actor, existing.getEmployee().getId());
        payscaleService.deletePayscale(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payscale> getPayscaleById(@PathVariable Long id) {
        var actor = tenantAccessService.requireCurrentEmployee();
        var existing = payscaleService.getPayscaleById(id);
        tenantAccessService.assertCanAccessEmployeeId(actor, existing.getEmployee().getId());
        return ResponseEntity.ok(existing);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<List<Payscale>> getAllPayscales(@RequestParam Long organizationId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(payscaleService.getPayscalesByOrganizationId(scopedOrg));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<List<Payscale>> getPayscalesByEmployeeId(@PathVariable Long employeeId) {
        tenantAccessService.assertCanAccessEmployeeId(tenantAccessService.requireCurrentEmployee(), employeeId);
        return ResponseEntity.ok(payscaleService.getPayscalesByEmployeeId(employeeId));
    }

    @GetMapping("/employee/{employeeId}/active")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payscale> getActivePayscaleByEmployeeId(@PathVariable Long employeeId) {
        tenantAccessService.assertCanAccessEmployeeId(tenantAccessService.requireCurrentEmployee(), employeeId);
        return ResponseEntity.ok(payscaleService.getActivePayscaleByEmployeeId(employeeId));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Void> deactivatePayscale(@PathVariable Long id) {
        var actor = tenantAccessService.requireCurrentEmployee();
        var existing = payscaleService.getPayscaleById(id);
        tenantAccessService.assertCanAccessEmployeeId(actor, existing.getEmployee().getId());
        payscaleService.deactivatePayscale(id);
        return ResponseEntity.ok().build();
    }
} 