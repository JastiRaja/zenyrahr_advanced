package com.zenyrahr.hrms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectRequestDTO {
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate deadline;
    private String status;
    private List<Long> employeeIds;
}
