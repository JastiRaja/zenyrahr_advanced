package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.PayrollGeneration;
import com.zenyrahr.hrms.model.Payroll;
import com.zenyrahr.hrms.service.PayrollService;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.dto.PayrollDeductionUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final TenantAccessService tenantAccessService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<PayrollGeneration> generatePayroll(
            @RequestParam String monthYear,
            @RequestParam String generatedBy,
            @RequestParam Long organizationId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(payrollService.generatePayroll(scopedOrg, monthYear, generatedBy));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<PayrollGeneration> approvePayroll(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        return ResponseEntity.ok(payrollService.approvePayroll(id, approvedBy));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<PayrollGeneration> rejectPayroll(
            @PathVariable Long id,
            @RequestParam String approvedBy,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(payrollService.rejectPayroll(id, approvedBy, rejectionReason));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<PayrollGeneration> getPayrollGenerationById(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayrollGenerationById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<List<PayrollGeneration>> getAllPayrolls(@RequestParam Long organizationId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(payrollService.getAllPayrolls(scopedOrg));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<List<PayrollGeneration>> getPayrollsByStatus(
            @PathVariable String status,
            @RequestParam Long organizationId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(payrollService.getPayrollsByStatus(scopedOrg, status));
    }

    @GetMapping("/month/{monthYear}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<List<PayrollGeneration>> getPayrollsByMonthYear(
            @PathVariable String monthYear,
            @RequestParam Long organizationId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Long scopedOrg = tenantAccessService.resolveOrganizationIdForScopedQuery(actor, organizationId);
        return ResponseEntity.ok(payrollService.getPayrollsByMonthYear(scopedOrg, monthYear));
    }

    @PostMapping("/generate-batch")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<?> generateBatchPayroll(@RequestParam String month) {
        try {
            YearMonth yearMonth = YearMonth.parse(month);
            return ResponseEntity.ok(payrollService.generatePayrollForAllEmployees(yearMonth));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/payslips")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin','manager','employee')")
    public ResponseEntity<List<Payroll>> getPayslipsByEmployeeId(@RequestParam Long employeeId) {
        var actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        if (!tenantAccessService.canManageEmployees(actor)
                && !tenantAccessService.isManager(actor)
                && !tenantAccessService.isMainAdmin(actor)) {
            if (!Objects.equals(actor.getId(), employeeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own payslips");
            }
        }
        return ResponseEntity.ok(payrollService.getPayrollsByEmployeeId(employeeId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','org_admin','hr')")
    public ResponseEntity<Void> deletePayroll(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/payslips/{id}/approve")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payroll> approvePayslip(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Payroll payroll = payrollService.getPayrollById(id);
        tenantAccessService.assertCanAccessEmployeeId(actor, payroll.getEmployeeId());
        payroll.setStatus("APPROVED");
        // Optionally set approvedBy, approvedAt, etc. if your model supports it
        return ResponseEntity.ok(payrollService.save(payroll));
    }

    @PutMapping("/payslips/{id}/deductions")
    @PreAuthorize("hasAnyAuthority('zenyrahr_admin','hr','org_admin')")
    public ResponseEntity<Payroll> updatePayrollDeductions(
            @PathVariable Long id,
            @RequestBody PayrollDeductionUpdateRequest request) {
        var actor = tenantAccessService.requireCurrentEmployee();
        Payroll payroll = payrollService.getPayrollById(id);
        tenantAccessService.assertCanAccessEmployeeId(actor, payroll.getEmployeeId());
        payroll.setEpfAmount(request.getEpfAmount());
        payroll.setHealthInsuranceDeduction(request.getHealthInsuranceDeduction());
        payroll.setProfessionalTax(request.getProfessionalTax());
        payroll.setOtherAllowances(request.getOtherDeductions()); // Adjust if you have a separate field
        // Recalculate total deductions and net pay
        double totalDeductions = 0.0;
        try {
            totalDeductions = Double.parseDouble(payroll.getEpfAmount())
                + Double.parseDouble(payroll.getProfessionalTax())
                + Double.parseDouble(payroll.getHealthInsuranceDeduction())
                + Double.parseDouble(payroll.getOtherAllowances());
        } catch (Exception e) {
            // handle parse error, optionally log
        }
        payroll.setTotalDeductions(String.format("%.2f", totalDeductions));
        double netPay = 0.0;
        try {
            netPay = Double.parseDouble(payroll.getGrossPay()) - totalDeductions;
        } catch (Exception e) {
            // handle parse error, optionally log
        }
        payroll.setNetPay(String.format("%.2f", netPay));
        return ResponseEntity.ok(payrollService.save(payroll));
    }
}