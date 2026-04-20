package com.zenyrahr.hrms.dto;

import java.util.UUID;

public class ApprovalDTO {
    private UUID employeeTaskId;
    private UUID approverId; // ID of the person who approves
    private boolean approved; // Whether the task was approved or not
    private String remarks;
    // Getters and Setters
}
