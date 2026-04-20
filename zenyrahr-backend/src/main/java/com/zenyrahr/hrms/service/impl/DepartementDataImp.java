package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.model.Department;
import com.zenyrahr.hrms.Repository.DepartmentRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.DepartmentDataService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartementDataImp implements DepartmentDataService {

    private final DepartmentRepository departmentRepository;
    private final SequenceService sequenceService;
    private final TenantAccessService tenantAccessService;
    private final OrganizationRepository organizationRepository;

    @Override
    public Department createDepartment(Department department) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long orgId = resolveOrgIdForActor(actor, department);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        department.setOrganization(org);
        department.setCode(sequenceService.getNextCode("DEPT"));
        return departmentRepository.save(department);
    }

    @Override
    public Optional<Department> getDepartmentById(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Optional<Department> existing = departmentRepository.findById(id);
        existing.ifPresent(department -> assertScoped(actor, department));
        return existing;
    }

    @Override
    public List<Department> getAllDepartments() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            return departmentRepository.findAll();
        }
        return departmentRepository.findByOrganization_Id(tenantAccessService.requireOrganizationId(actor));
    }

    @Override
    public List<Department> getAllDepartmentData() {
        return getAllDepartments();
    }

    @Override
    public Department updateDepartment(Long id, Department department) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        assertScoped(actor, existing);
        Long orgId = resolveOrgIdForActor(actor, department);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        existing.setName(department.getName());
        existing.setDescription(department.getDescription());
        existing.setOrganization(org);
        return departmentRepository.save(existing);
    }

    @Override
    public void deleteDepartment(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        assertScoped(actor, existing);
        departmentRepository.deleteById(id);
    }

    private void assertScoped(Employee actor, Department department) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        Long deptOrgId = department.getOrganization() != null ? department.getOrganization().getId() : null;
        if (!actorOrgId.equals(deptOrgId)) {
            throw new RuntimeException("Cross-organization access is not allowed");
        }
    }

    private Long resolveOrgIdForActor(Employee actor, Department incoming) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (incoming.getOrganization() == null || incoming.getOrganization().getId() == null) {
                throw new RuntimeException("Organization is required");
            }
            return incoming.getOrganization().getId();
        }
        return tenantAccessService.requireOrganizationId(actor);
    }
}
