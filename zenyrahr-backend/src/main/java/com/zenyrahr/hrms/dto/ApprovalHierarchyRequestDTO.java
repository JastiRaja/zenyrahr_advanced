package com.zenyrahr.hrms.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApprovalHierarchyRequestDTO {
    private List<ApprovalHierarchyModuleDTO> modules = new ArrayList<>();
}

