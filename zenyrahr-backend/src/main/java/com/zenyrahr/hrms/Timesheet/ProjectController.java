package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.dto.ProjectRequestDTO;
import com.zenyrahr.hrms.dto.ProjectResponseDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TenantAccessService tenantAccessService;

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@RequestBody ProjectRequestDTO project) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        Project createdProject = projectService.createProject(actor, project);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponseDTO.fromProject(createdProject));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(projectService.getAllProjectResponses(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return projectService.getProjectById(actor, id)
                .map(ProjectResponseDTO::fromProject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(@PathVariable Long id, @RequestBody ProjectRequestDTO project) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        return ResponseEntity.ok(ProjectResponseDTO.fromProject(projectService.updateProject(actor, id, project)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        projectService.deleteProject(actor, id);
        return ResponseEntity.noContent().build();
    }
}


