package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.LocationRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Location;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.LocationService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {


    private final LocationRepository locationRepository;
    private final SequenceService sequenceService;
    private final TenantAccessService tenantAccessService;
    private final OrganizationRepository organizationRepository;

    @Override
    public Location createLocation(Location location) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long orgId = resolveOrgIdForActor(actor, location);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        location.setOrganization(org);
        location.setCreatedAt(LocalDateTime.now());
        location.setActive(true);
        location.setCode(sequenceService.getNextCode("LOCA"));
        return locationRepository.save(location);
    }

    @Override
    public Optional<Location> getLocationById(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Optional<Location> location = locationRepository.findById(id);
        location.ifPresent(item -> assertScoped(actor, item));
        return location;
    }

    @Override
    public List<Location> getAllLocation() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            return locationRepository.findAll();
        }
        return locationRepository.findByOrganization_Id(tenantAccessService.requireOrganizationId(actor));
    }

    @Override
    public Location updateLocation(Long id, Location locationDetails) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return locationRepository.findById(id).map(location -> {
            assertScoped(actor, location);
            location.setName(locationDetails.getName());
            location.setActive(locationDetails.getActive());
            if (tenantAccessService.isMainAdmin(actor)
                    && locationDetails.getOrganization() != null
                    && locationDetails.getOrganization().getId() != null) {
                Organization org = organizationRepository.findById(locationDetails.getOrganization().getId())
                        .orElseThrow(() -> new RuntimeException("Organization not found"));
                location.setOrganization(org);
            }
            location.setUpdatedAt(LocalDateTime.now());
            return locationRepository.save(location);
        }).orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    }

    @Override
    public void deleteLocation(Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        assertScoped(actor, existing);
        locationRepository.deleteById(id);
    }


    public int add(int a, int b){
        return a+b;
    }

    public int add(int a, int b, int c){
        return a+b+c;
    }

    private void assertScoped(Employee actor, Location location) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return;
        }
        Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
        Long locationOrgId = location.getOrganization() != null ? location.getOrganization().getId() : null;
        if (!actorOrgId.equals(locationOrgId)) {
            throw new RuntimeException("Cross-organization access is not allowed");
        }
    }

    private Long resolveOrgIdForActor(Employee actor, Location incoming) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (incoming.getOrganization() == null || incoming.getOrganization().getId() == null) {
                throw new RuntimeException("Organization is required");
            }
            return incoming.getOrganization().getId();
        }
        return tenantAccessService.requireOrganizationId(actor);
    }
}
