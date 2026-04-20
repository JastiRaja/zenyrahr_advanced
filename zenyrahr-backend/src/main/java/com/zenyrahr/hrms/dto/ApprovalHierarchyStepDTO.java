package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.ApproverType;
import lombok.Data;

@Data
public class ApprovalHierarchyStepDTO {
    private Integer levelNo;
    private String requesterRole;
    private ApproverType approverType;
    private String approverRole;
    private Long approverUserId;
    private String approverUserName;
}

