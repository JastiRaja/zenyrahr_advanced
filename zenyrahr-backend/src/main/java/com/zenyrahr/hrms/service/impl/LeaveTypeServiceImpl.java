package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.LeaveTypeRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.LeaveType;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.LeaveTypeService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@Service
public class LeaveTypeServiceImpl implements LeaveTypeService {
    private final LeaveTypeRepository leaveTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public LeaveType createLeaveType(Employee actor, Long organizationId, LeaveType leaveType) {
        Long resolvedOrgId = resolveOrgId(actor, organizationId, true);
        if (leaveType.getName() == null || leaveType.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Leave type name is required");
        }
        if (leaveTypeRepository.existsByNameIgnoreCaseAndOrganization_Id(leaveType.getName().trim(), resolvedOrgId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Leave type already exists for this organization");
        }
        Organization org = organizationRepository.findById(resolvedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        leaveType.setName(leaveType.getName().trim());
        leaveType.setOrganization(org);
        return leaveTypeRepository.save(leaveType);
    }

    @Override
    public List<LeaveType> getAllLeaveTypes(Employee actor, Long organizationId) {
        Long resolvedOrgId = resolveOrgId(actor, organizationId, false);
        if (resolvedOrgId == null) {
            return leaveTypeRepository.findAll();
        }
        return leaveTypeRepository.findByOrganization_Id(resolvedOrgId);
    }

    @Override
    public LeaveType getLeaveTypeById(Employee actor, Long organizationId, Long id) {
        Long resolvedOrgId = resolveOrgId(actor, organizationId, false);
        if (resolvedOrgId == null) {
            return leaveTypeRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Leave type not found"));
        }
        return leaveTypeRepository.findByIdAndOrganization_Id(id, resolvedOrgId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Leave type not found"));
    }

    @Override
    public LeaveType updateLeaveType(Employee actor, Long organizationId, Long id, LeaveType leaveType) {
        LeaveType existingLeaveType = getLeaveTypeById(actor, organizationId, id);
        if (leaveType.getName() == null || leaveType.getName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Leave type name is required");
        }
        existingLeaveType.setName(leaveType.getName());
        existingLeaveType.setDefaultBalance(leaveType.getDefaultBalance());
        return leaveTypeRepository.save(existingLeaveType);
    }

    @Override
    public void deleteLeaveType(Employee actor, Long organizationId, Long id) {
        LeaveType existingLeaveType = getLeaveTypeById(actor, organizationId, id);
        leaveTypeRepository.delete(existingLeaveType);
    }

    private Long resolveOrgId(Employee actor, Long organizationId, boolean requireManagePermission) {
        tenantAccessService.assertOrganizationActive(actor);
        if (requireManagePermission && !tenantAccessService.canManageEmployees(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to manage leave types");
        }

        if (tenantAccessService.isMainAdmin(actor)) {
            if (organizationId == null) {
                if (!requireManagePermission) {
                    return null;
                }
                throw new ResponseStatusException(BAD_REQUEST, "organizationId is required for main admin");
            }
            return organizationId;
        }
        return tenantAccessService.requireOrganizationId(actor);
    }
}
