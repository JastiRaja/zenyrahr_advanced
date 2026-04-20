package com.zenyrahr.hrms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationPolicyDocumentDTO {
    private Long id;
    private String title;
    private String fileName;
    private String fileUrl;
    private Long fileSizeBytes;
    private Long organizationId;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
