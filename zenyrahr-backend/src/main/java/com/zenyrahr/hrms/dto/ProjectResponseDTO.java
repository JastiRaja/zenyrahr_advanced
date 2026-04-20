package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.Timesheet.Project;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ProjectResponseDTO {
    private Long id;
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate deadline;
    private String status;
    private List<Long> employeeIds;

    public static ProjectResponseDTO fromProject(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getProjectName(),
                project.getDescription(),
                project.getStartDate(),
                project.getDeadline(),
                project.getStatus(),
                project.getEmployees() == null
                        ? List.of()
                        : project.getEmployees().stream().map(employee -> employee.getId()).toList()
        );
    }
}
