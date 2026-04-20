package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.dto.AttendanceDTO;
import com.zenyrahr.hrms.dto.AttendanceStatsDTO;
import com.zenyrahr.hrms.dto.PunchRequestDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.AttendanceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/payroll/attendance")
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private TenantAccessService tenantAccessService;

    private Long resolveOrgIdForManager(Employee actor, Long organizationId) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return organizationId;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        if (organizationId != null && !Objects.equals(actorOrgId, organizationId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        return actorOrgId;
    }

    @PostMapping("/batch")
    public ResponseEntity<?> markBatchAttendance(@RequestBody List<AttendanceDTO> attendanceList) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        for (AttendanceDTO dto : attendanceList) {
            tenantAccessService.assertCanAccessEmployeeId(actor, dto.getEmployeeId());
        }
        attendanceService.saveBatchAttendance(attendanceList);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<AttendanceDTO>> getAttendance(
        @RequestParam Long employeeId,
        @RequestParam String month,
        @RequestParam String year
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        List<AttendanceDTO> records = attendanceService.getAttendanceForEmployeeAndMonth(employeeId, month, year);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<AttendanceStatsDTO>> getAttendanceStats(
        @RequestParam(defaultValue = "monthly") String period,
        @RequestParam(required = false) String department,
        @RequestParam(required = false) Long employeeId,
        @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Long scopedOrgId = resolveOrgIdForManager(actor, organizationId);
        Long scopedEmployeeId = employeeId;
        if (!tenantAccessService.canManageEmployees(actor)) {
            scopedEmployeeId = actor.getId();
        } else if (scopedEmployeeId != null) {
            tenantAccessService.assertCanAccessEmployeeId(actor, scopedEmployeeId);
        }
        List<AttendanceStatsDTO> stats = attendanceService.getAttendanceStats(period, department, scopedOrgId, scopedEmployeeId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/today-punch-ins")
    public ResponseEntity<Map<String, Long>> getTodayPunchIns(
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (!tenantAccessService.canManageEmployees(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "You are not allowed to view punch-in summary");
        }
        Long scopedOrgId = resolveOrgIdForManager(actor, organizationId);
        long totalPunchIns = attendanceService.getTodayPunchInCount(scopedOrgId);
        return ResponseEntity.ok(Map.of("totalPunchIns", totalPunchIns));
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceDTO> getTodayAttendance(@RequestParam Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        AttendanceDTO dto = attendanceService.getTodayAttendance(employeeId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/punch-in")
    public ResponseEntity<?> punchIn(
        @RequestParam Long employeeId,
        @RequestBody(required = false) PunchRequestDTO body
    ) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
            PunchRequestDTO req = body != null ? body : new PunchRequestDTO();
            return ResponseEntity.ok(attendanceService.punchIn(employeeId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/punch-out")
    public ResponseEntity<?> punchOut(
        @RequestParam Long employeeId,
        @RequestBody(required = false) PunchRequestDTO body
    ) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
            PunchRequestDTO req = body != null ? body : new PunchRequestDTO();
            return ResponseEntity.ok(attendanceService.punchOut(employeeId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
} 