package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.dto.ApprovalHierarchyModuleDTO;
import com.zenyrahr.hrms.dto.ApprovalHierarchyRequestDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-hierarchy")
@RequiredArgsConstructor
public class ApprovalHierarchyController {
    private final ApprovalHierarchyService approvalHierarchyService;
    private final TenantAccessService tenantAccessService;

    @GetMapping("/current")
    public ResponseEntity<List<ApprovalHierarchyModuleDTO>> getCurrent() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(approvalHierarchyService.getCurrentHierarchy(actor));
    }

    @PutMapping("/current")
    public ResponseEntity<List<ApprovalHierarchyModuleDTO>> updateCurrent(@RequestBody ApprovalHierarchyRequestDTO payload) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(approvalHierarchyService.saveCurrentHierarchy(actor, payload));
    }
}

