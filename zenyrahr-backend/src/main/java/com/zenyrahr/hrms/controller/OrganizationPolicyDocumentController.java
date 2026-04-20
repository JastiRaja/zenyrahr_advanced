package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.dto.OrganizationPolicyDocumentDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.OrganizationPolicyDocumentService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/organization-policies")
@RequiredArgsConstructor
public class OrganizationPolicyDocumentController {

    private final OrganizationPolicyDocumentService policyService;
    private final TenantAccessService tenantAccessService;

    @GetMapping
    public ResponseEntity<List<OrganizationPolicyDocumentDTO>> listPolicies() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(policyService.listPoliciesForActor(actor));
    }

    @PostMapping
    public ResponseEntity<OrganizationPolicyDocumentDTO> createPolicy(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) throws IOException {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(policyService.createPolicy(actor, title, file));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationPolicyDocumentDTO> updatePolicy(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(policyService.updatePolicy(actor, id, title, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        policyService.deletePolicy(actor, id);
        return ResponseEntity.noContent().build();
    }
}
