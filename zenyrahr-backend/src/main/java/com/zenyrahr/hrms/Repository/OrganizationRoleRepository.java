package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, Long> {
    List<OrganizationRole> findByOrganization_IdOrderByNameAsc(Long organizationId);

    boolean existsByNameIgnoreCaseAndOrganization_Id(String name, Long organizationId);

    long countByOrganization_Id(Long organizationId);

    Optional<OrganizationRole> findByNameIgnoreCaseAndOrganization_Id(String name, Long organizationId);
}
