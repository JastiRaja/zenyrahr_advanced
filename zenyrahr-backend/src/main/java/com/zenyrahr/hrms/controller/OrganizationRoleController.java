package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.OrganizationRole;
import com.zenyrahr.hrms.service.OrganizationRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class OrganizationRoleController {

    private final OrganizationRoleService organizationRoleService;

    @GetMapping
    public ResponseEntity<List<OrganizationRole>> getRoles(
            @RequestParam(required = false) Long organizationId
    ) {
        return ResponseEntity.ok(organizationRoleService.getRoles(organizationId));
    }

    @PostMapping
    public ResponseEntity<OrganizationRole> createRole(
            @RequestBody Map<String, String> payload,
            @RequestParam(required = false) Long organizationId
    ) {
        return ResponseEntity.ok(organizationRoleService.createRole(organizationId, payload.get("name")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId
    ) {
        organizationRoleService.deleteRole(id, organizationId);
        return ResponseEntity.noContent().build();
    }
}
