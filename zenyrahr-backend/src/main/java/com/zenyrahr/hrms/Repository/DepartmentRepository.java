package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    java.util.List<Department> findByOrganization_Id(Long organizationId);
}
