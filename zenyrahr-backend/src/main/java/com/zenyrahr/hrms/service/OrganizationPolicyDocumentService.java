package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.OrganizationPolicyDocumentRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.dto.OrganizationPolicyDocumentDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.model.OrganizationPolicyDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrganizationPolicyDocumentService {

    private final OrganizationPolicyDocumentRepository policyRepository;
    private final OrganizationRepository organizationRepository;
    private final LocalFileStorageService localFileStorageService;

    @Transactional(readOnly = true)
    public List<OrganizationPolicyDocumentDTO> listPoliciesForActor(Employee actor) {
        Long organizationId = requireActorOrganizationId(actor);
        return policyRepository.findByOrganization_IdOrderByUpdatedAtDesc(organizationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrganizationPolicyDocumentDTO createPolicy(Employee actor, String title, MultipartFile file) throws IOException {
        assertCanManagePolicies(actor);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file is required");
        }
        if (!isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }

        Organization organization = resolveActorOrganization(actor);
        String normalizedTitle = normalizeTitle(title, file.getOriginalFilename());
        String fileUrl = localFileStorageService.storeOrganizationPolicyPdf(
                file,
                organization.getName(),
                normalizedTitle
        );

        OrganizationPolicyDocument document = new OrganizationPolicyDocument();
        document.setOrganization(organization);
        document.setUploadedBy(actor);
        document.setTitle(normalizedTitle);
        document.setFileName(file.getOriginalFilename() == null ? "policy.pdf" : file.getOriginalFilename());
        document.setFileUrl(fileUrl);
        document.setFileSizeBytes(file.getSize());
        return toDto(policyRepository.save(document));
    }

    @Transactional
    public OrganizationPolicyDocumentDTO updatePolicy(
            Employee actor,
            Long policyId,
            String newTitle,
            MultipartFile replacementFile
    ) throws IOException {
        assertCanManagePolicies(actor);
        OrganizationPolicyDocument document = getScopedDocument(actor, policyId);

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            document.setTitle(newTitle.trim());
        }

        if (replacementFile != null && !replacementFile.isEmpty()) {
            if (!isPdf(replacementFile)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
            }
            localFileStorageService.deleteByRelativeUrl(document.getFileUrl());
            String fileUrl = localFileStorageService.storeOrganizationPolicyPdf(
                    replacementFile,
                    document.getOrganization().getName(),
                    document.getTitle()
            );
            document.setFileUrl(fileUrl);
            document.setFileName(
                    replacementFile.getOriginalFilename() == null ? "policy.pdf" : replacementFile.getOriginalFilename()
            );
            document.setFileSizeBytes(replacementFile.getSize());
        }

        return toDto(policyRepository.save(document));
    }

    @Transactional
    public void deletePolicy(Employee actor, Long policyId) {
        assertCanManagePolicies(actor);
        OrganizationPolicyDocument document = getScopedDocument(actor, policyId);
        localFileStorageService.deleteByRelativeUrl(document.getFileUrl());
        policyRepository.delete(document);
    }

    private Organization resolveActorOrganization(Employee actor) {
        Long orgId = requireActorOrganizationId(actor);
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found"));
    }

    private Long requireActorOrganizationId(Employee actor) {
        if (actor == null || actor.getOrganization() == null || actor.getOrganization().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization assignment is required");
        }
        return actor.getOrganization().getId();
    }

    private OrganizationPolicyDocument getScopedDocument(Employee actor, Long policyId) {
        OrganizationPolicyDocument document = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy document not found"));
        Long actorOrgId = requireActorOrganizationId(actor);
        Long documentOrgId = document.getOrganization() == null ? null : document.getOrganization().getId();
        if (!Objects.equals(actorOrgId, documentOrgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-organization access is not allowed");
        }
        return document;
    }

    private void assertCanManagePolicies(Employee actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        String role = actor.getRole() == null ? "" : actor.getRole().toLowerCase();
        if (!"hr".equals(role) && !"org_admin".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR or organization admin can manage policy documents");
        }
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && "application/pdf".equalsIgnoreCase(contentType.trim())) {
            return true;
        }
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".pdf");
    }

    private String normalizeTitle(String title, String fallbackFileName) {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (fallbackFileName == null || fallbackFileName.isBlank()) {
            return "Policy Document";
        }
        return fallbackFileName.replaceAll("\\.pdf$", "").trim();
    }

    private OrganizationPolicyDocumentDTO toDto(OrganizationPolicyDocument document) {
        Employee uploader = document.getUploadedBy();
        String uploaderName = uploader == null
                ? ""
                : ((uploader.getFirstName() == null ? "" : uploader.getFirstName()) + " " +
                (uploader.getLastName() == null ? "" : uploader.getLastName())).trim();
        return OrganizationPolicyDocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSizeBytes(document.getFileSizeBytes())
                .organizationId(document.getOrganization() == null ? null : document.getOrganization().getId())
                .uploadedById(uploader == null ? null : uploader.getId())
                .uploadedByName(uploaderName)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
