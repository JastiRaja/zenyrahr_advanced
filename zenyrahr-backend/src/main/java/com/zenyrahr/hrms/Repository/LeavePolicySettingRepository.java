package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.LeavePolicySetting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePolicySettingRepository extends JpaRepository<LeavePolicySetting, Long> {
    @EntityGraph(attributePaths = {"allocations", "allocations.leaveType"})
    Optional<LeavePolicySetting> findByOrganization_Id(Long organizationId);

    @EntityGraph(attributePaths = {"allocations", "allocations.leaveType"})
    Optional<LeavePolicySetting> findByOrganizationIsNull();
}
