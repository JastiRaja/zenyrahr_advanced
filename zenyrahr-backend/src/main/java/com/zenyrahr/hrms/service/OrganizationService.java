package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Organization;

import java.util.List;

public interface OrganizationService {
    List<Organization> getAllOrganizations();
    Organization getOrganizationById(Long id);
    Organization createOrganization(Organization organization);
    Organization updateOrganization(Long id, Organization organization);
    void deleteOrganization(Long id);
    Organization setOrganizationActive(Long id, boolean active);
    int assignEmployeesToOrganization(Long organizationId, List<Long> employeeIds);
    void assertCanAddActiveUsers(Long organizationId, long additionalUsers);
}
