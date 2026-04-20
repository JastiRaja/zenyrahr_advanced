package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.LeaveType;
import com.zenyrahr.hrms.service.LeaveTypeService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/leave-types")
public class LeaveTypeController {
    private final LeaveTypeService leaveTypeService;
    private final TenantAccessService tenantAccessService;

    private Employee requireLeaveTypeManager() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (!tenantAccessService.canManageEmployees(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only admin/hr/org admin can manage leave types");
        }
        return actor;
    }

    @PostMapping
    public ResponseEntity<LeaveType> createLeaveType(
            @RequestBody LeaveType leaveType,
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = requireLeaveTypeManager();
        return ResponseEntity.ok(leaveTypeService.createLeaveType(actor, organizationId, leaveType));
    }

    @GetMapping
    public ResponseEntity<List<LeaveType>> getAllLeaveTypes(
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(leaveTypeService.getAllLeaveTypes(actor, organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveType> getLeaveTypeById(
            @PathVariable Long id,
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(leaveTypeService.getLeaveTypeById(actor, organizationId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveType> updateLeaveType(
            @PathVariable Long id,
            @RequestBody LeaveType leaveType,
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = requireLeaveTypeManager();
        return ResponseEntity.ok(leaveTypeService.updateLeaveType(actor, organizationId, id, leaveType));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveType(
            @PathVariable Long id,
            @RequestParam(value = "organizationId", required = false) Long organizationId
    ) {
        Employee actor = requireLeaveTypeManager();
        leaveTypeService.deleteLeaveType(actor, organizationId, id);
        return ResponseEntity.noContent().build();
    }
}