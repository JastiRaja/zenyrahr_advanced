package com.zenyrahr.hrms.Repository;


import com.zenyrahr.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    List<Employee> findByReportingManager_Id(Long managerId);
    List<Employee> findByOrganization_Id(Long organizationId);
    boolean existsByCode(String code);
    boolean existsByRoleIgnoreCase(String role);
    boolean existsByRoleIgnoreCaseAndOrganization_Id(String role, Long organizationId);
    long countByOrganization_Id(Long organizationId);
    long countByOrganization_IdAndActiveTrue(Long organizationId);
}
