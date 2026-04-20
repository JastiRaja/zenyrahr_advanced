package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.model.Designation;
import com.zenyrahr.hrms.Repository.DesignationRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.DesignationDataService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DesignationDataImpl implements DesignationDataService {

    private final DesignationRepository designationRepository;
    private final SequenceService sequenceService;
    private final TenantAccessService tenantAccessService;
    private final OrganizationRepository organizationRepository;

    @Override
    public Designation createDesignation(Designation designation) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long orgId = resolveOrgIdForActor(actor, designation);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        designation.setOrganization(org);
        designation.setCode(sequenceService.getNextCode("DESG"));
        return designationRepository.save(designation);
    }

    @Override
    public Optional<Designation> getDesignationById(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Optional<Designation> designation = designationRepository.findById(id);
        designation.ifPresent(item -> assertScoped(actor, item));
        return designation;
    }

    @Override
    public List<Designation> getAllDesignations() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            return designationRepository.findAll();
        }
        return designationRepository.findByOrganization_Id(tenantAccessService.requireOrganizationId(actor));
    }

    @Override
    public Designation updateDesignation(Long id, Designation designation) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Designation existing = designationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Designation not found"));
        assertScoped(actor, existing);
        Long orgId = resolveOrgIdForActor(actor, designation);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        existing.setName(designation.getName());
        existing.setDescription(designation.getDescription());
        existing.setOrganization(org);
        return designationRepository.save(existing);
    }

    @Override
    public void deleteDesignation(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Designation existing = designationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Designation not found"));
        assertScoped(actor, existing);
        designationRepository.deleteById(id);
    }

    private void assertScoped(Employee actor, Designation designation) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        Long designationOrgId = designation.getOrganization() != null ? designation.getOrganization().getId() : null;
        if (!actorOrgId.equals(designationOrgId)) {
            throw new RuntimeException("Cross-organization access is not allowed");
        }
    }

    private Long resolveOrgIdForActor(Employee actor, Designation incoming) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (incoming.getOrganization() == null || incoming.getOrganization().getId() == null) {
                throw new RuntimeException("Organization is required");
            }
            return incoming.getOrganization().getId();
        }
        return tenantAccessService.requireOrganizationId(actor);
    }
}
