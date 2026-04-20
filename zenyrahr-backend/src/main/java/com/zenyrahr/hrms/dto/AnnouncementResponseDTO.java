package com.zenyrahr.hrms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnnouncementResponseDTO {
    private Long id;
    private String title;
    private String message;
    private String postedByName;
    private String postedByRole;
    private Boolean active;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
