package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.ProcessType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
@Getter
@Setter
public class TemplateDTO {
    private String name;
    private String description;
    private List<UUID> taskIds; // IDs of tasks associated with the template
    // Getters and Setters
    private List<TaskDTO> tasks; // List of tasks for this template

    private ProcessType processType;  // Specify process type



}
