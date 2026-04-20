package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.OrganizationPolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationPolicyDocumentRepository extends JpaRepository<OrganizationPolicyDocument, Long> {
    List<OrganizationPolicyDocument> findByOrganization_IdOrderByUpdatedAtDesc(Long organizationId);
}
