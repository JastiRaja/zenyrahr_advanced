package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.ApprovalModule;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApprovalHierarchyModuleDTO {
    private ApprovalModule module;
    private List<ApprovalHierarchyStepDTO> steps = new ArrayList<>();
}

