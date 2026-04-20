package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    List<LeaveType> findByOrganization_Id(Long organizationId);
    Optional<LeaveType> findByIdAndOrganization_Id(Long id, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganization_Id(String name, Long organizationId);
}
