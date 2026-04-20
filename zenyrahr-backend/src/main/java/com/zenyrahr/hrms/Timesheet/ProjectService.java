package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.dto.ProjectResponseDTO;
import com.zenyrahr.hrms.dto.ProjectRequestDTO;
import com.zenyrahr.hrms.model.Employee;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    Project createProject(Employee actor, ProjectRequestDTO project);
    List<Project> getAllProjects(Employee actor);
    List<ProjectResponseDTO> getAllProjectResponses(Employee actor);
    List<ProjectResponseDTO> getAssignedProjectResponses(Employee actor, Long employeeId);
    Optional<Project> getProjectById(Employee actor, Long id);
    Project updateProject(Employee actor, Long id, ProjectRequestDTO project);
    void deleteProject(Employee actor, Long id);
}
