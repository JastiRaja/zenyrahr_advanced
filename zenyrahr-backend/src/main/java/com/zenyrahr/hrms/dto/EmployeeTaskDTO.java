package com.zenyrahr.hrms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmployeeTaskDTO {
    private Long taskId;
    private Long employeeId;
    private String status; // e.g., "InProgress", "Completed", "Pending"
    private String remarks;
    // Getters and Setters

    private String name; // Template name
    private List<TaskDTO> tasks; // List of tasks for this template
}
