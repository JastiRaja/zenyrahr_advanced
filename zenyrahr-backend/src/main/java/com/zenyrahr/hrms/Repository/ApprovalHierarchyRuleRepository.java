package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.ApprovalHierarchyRule;
import com.zenyrahr.hrms.model.ApprovalModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalHierarchyRuleRepository extends JpaRepository<ApprovalHierarchyRule, Long> {
    List<ApprovalHierarchyRule> findByOrganization_IdAndModuleAndActiveTrueOrderByLevelNoAsc(Long organizationId, ApprovalModule module);
    List<ApprovalHierarchyRule> findByOrganization_IdAndModuleOrderByLevelNoAsc(Long organizationId, ApprovalModule module);
    void deleteByOrganization_IdAndModule(Long organizationId, ApprovalModule module);
}

