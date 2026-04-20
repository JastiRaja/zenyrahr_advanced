package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.WorkShift;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.WorkShiftDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/WorkShift")
@RequiredArgsConstructor
public class WorkShiftController {

    private final WorkShiftDataService workShiftDataService;
    private final TenantAccessService tenantAccessService;

    private void requireMainAdmin() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (!tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only main admin can manage work shifts");
        }
    }

    @GetMapping
    public ResponseEntity<List<WorkShift>> getAllWorkShifts() {
        return ResponseEntity.ok(workShiftDataService.getAllWorkShifts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkShift> getWorkShiftById(@PathVariable Long id) {
        return workShiftDataService.getWorkShiftById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WorkShift> createWorkShift(@RequestBody WorkShift workShift) {
        requireMainAdmin();
        return ResponseEntity.ok(workShiftDataService.createWorkShift(workShift));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkShift> updateWorkShift(@PathVariable Long id, @RequestBody WorkShift workShift) {
        requireMainAdmin();
        return ResponseEntity.ok(workShiftDataService.updateWorkShift(id, workShift));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkShift(@PathVariable Long id) {
        requireMainAdmin();
        workShiftDataService.deleteWorkShift(id);
        return ResponseEntity.noContent().build();
    }
}
