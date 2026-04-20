package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.dto.EmployeeLeaveBalanceDTO;
import com.zenyrahr.hrms.dto.CommonLeavePolicyRequest;
import com.zenyrahr.hrms.dto.CommonLeavePolicyResponse;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeLeaveBalance;
import com.zenyrahr.hrms.service.EmployeeLeaveBalanceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/leave-balances")
public class EmployeeLeaveBalanceController {
    private final EmployeeLeaveBalanceService leaveBalanceService;
    private final TenantAccessService tenantAccessService;

    @PostMapping
    public ResponseEntity<EmployeeLeaveBalance> createLeaveBalance(@RequestBody EmployeeLeaveBalance leaveBalance) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, leaveBalance.getEmployee().getId());
        return ResponseEntity.ok(leaveBalanceService.createLeaveBalance(leaveBalance));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeLeaveBalanceDTO>> getLeaveBalancesByEmployee(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalancesByEmployee(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeLeaveBalanceDTO>> getAllLeaveBalance(){
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<EmployeeLeaveBalanceDTO> items = leaveBalanceService.getAllLeaveBalance();
        if (!tenantAccessService.isMainAdmin(actor)) {
            items = items.stream()
                    .filter(item -> {
                        try {
                            tenantAccessService.assertCanAccessEmployeeId(actor, item.getEmployeeId());
                            return true;
                        } catch (Exception ignored) {
                            return false;
                        }
                    })
                    .toList();
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/common-policy")
    public ResponseEntity<CommonLeavePolicyResponse> getCommonPolicy(
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanManageEmployees(actor);
        return ResponseEntity.ok(leaveBalanceService.getCommonLeavePolicy(actor, organizationId));
    }

    @PostMapping("/common-policy/apply")
    public ResponseEntity<CommonLeavePolicyResponse> applyCommonPolicy(
            @RequestBody CommonLeavePolicyRequest request,
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanManageEmployees(actor);
        return ResponseEntity.ok(leaveBalanceService.applyCommonLeavePolicy(actor, organizationId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeLeaveBalance> updateLeaveBalance(@PathVariable Long id, @RequestParam Integer newBalance) {
        return ResponseEntity.ok(leaveBalanceService.updateLeaveBalance(id, newBalance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveBalance(@PathVariable Long id) {
        leaveBalanceService.deleteLeaveBalance(id);
        return ResponseEntity.noContent().build();
    }
}



