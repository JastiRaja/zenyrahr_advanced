package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeTable;
import com.zenyrahr.hrms.service.EmployeeTableService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/employee-table")
@RequiredArgsConstructor
public class EmployeeTableController {

    private final EmployeeTableService employeeTableService;
    private final TenantAccessService tenantAccessService;

    private void requireMainAdmin() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (!tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only main admin can access employee table management");
        }
    }

    @PostMapping
    public ResponseEntity<EmployeeTable> createEmployeeTable(@RequestBody EmployeeTable employeeTable) {
        requireMainAdmin();
        return ResponseEntity.ok(employeeTableService.createEmployeeTable(employeeTable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeTable> getEmployeeTableById(@PathVariable Long id) {
        requireMainAdmin();
        return employeeTableService.getEmployeeTableById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EmployeeTable>> getAllEmployeeTables() {
        requireMainAdmin();
        return ResponseEntity.ok(employeeTableService.getAllEmployeeTables());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeTable> updateEmployeeTable(@PathVariable Long id, @RequestBody EmployeeTable employeeTable) {
        requireMainAdmin();
        return ResponseEntity.ok(employeeTableService.updateEmployeeTable(id, employeeTable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployeeTable(@PathVariable Long id) {
        requireMainAdmin();
        employeeTableService.deleteEmployeeTable(id);
        return ResponseEntity.noContent().build();
    }
}
