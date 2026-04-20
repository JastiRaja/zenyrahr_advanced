package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Holiday;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.HolidayService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;
    private final TenantAccessService tenantAccessService;

    private Employee requireHolidayManager() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (!tenantAccessService.canManageEmployees(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only admin/hr/org admin can manage holidays");
        }
        return actor;
    }

    private Long resolveOrgId(Employee actor, Long organizationId) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (organizationId == null) {
                throw new ResponseStatusException(BAD_REQUEST, "organizationId is required for main admin");
            }
            return organizationId;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        if (organizationId != null && !Objects.equals(actorOrgId, organizationId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        return actorOrgId;
    }

    @GetMapping("/api/holidays")
    public ResponseEntity<List<Holiday>> getPublicHolidays(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        Long resolvedOrgId = resolveOrgId(actor, organizationId);
        return ResponseEntity.ok(holidayService.getHolidaysByYear(resolvedOrgId, resolvedYear));
    }

    @GetMapping("/api/admin/holidays")
    public ResponseEntity<List<Holiday>> getAdminHolidays(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long organizationId
    ) {
        Employee actor = requireHolidayManager();
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        Long resolvedOrgId = resolveOrgId(actor, organizationId);
        return ResponseEntity.ok(holidayService.getHolidaysByYear(resolvedOrgId, resolvedYear));
    }

    @PostMapping("/api/admin/holidays")
    public ResponseEntity<?> addHoliday(
            @RequestBody Holiday holiday,
            @RequestParam(required = false) Long organizationId
    ) {
        try {
            Employee actor = requireHolidayManager();
            Long resolvedOrgId = resolveOrgId(actor, organizationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.addHoliday(resolvedOrgId, holiday));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/api/admin/holidays/bulk")
    public ResponseEntity<?> addHolidays(
            @RequestBody List<Holiday> holidays,
            @RequestParam(required = false) Long organizationId
    ) {
        try {
            Employee actor = requireHolidayManager();
            Long resolvedOrgId = resolveOrgId(actor, organizationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.addHolidays(resolvedOrgId, holidays));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/holidays/{id}")
    public ResponseEntity<?> deleteHoliday(
            @PathVariable Long id,
            @RequestParam(required = false) Long organizationId
    ) {
        try {
            Employee actor = requireHolidayManager();
            Long resolvedOrgId = resolveOrgId(actor, organizationId);
            holidayService.deleteHoliday(resolvedOrgId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }
}
