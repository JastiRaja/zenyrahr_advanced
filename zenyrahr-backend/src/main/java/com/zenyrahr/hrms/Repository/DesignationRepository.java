package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    java.util.List<Designation> findByOrganization_Id(Long organizationId);
}
