package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.dto.ProjectRequestDTO;
import com.zenyrahr.hrms.dto.ProjectResponseDTO;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Project createProject(Employee actor, ProjectRequestDTO project) {
        tenantAccessService.assertCanManageEmployees(actor);
        Project entity = new Project();
        List<Employee> resolvedEmployees = applyProjectPayloadPrepare(actor, entity, project);
        Project saved = projectRepository.save(entity);
        syncProjectEmployeesOnOwnerSide(saved, resolvedEmployees);
        return projectRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public List<Project> getAllProjects(Employee actor) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return projectRepository.findAll();
        }
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        return projectRepository.findByOrganization_Id(orgId);
    }

    @Override
    public List<ProjectResponseDTO> getAllProjectResponses(Employee actor) {
        String baseSql = """
                SELECT p.id, p.project_name, p.description, p.start_date, p.deadline, p.status
                FROM project p
                """;

        if (tenantAccessService.isMainAdmin(actor)) {
            return jdbcTemplate.query(
                    baseSql + " ORDER BY p.id DESC",
                    (rs, rowNum) -> toProjectResponseDTO(
                            rs.getLong("id"),
                            rs.getString("project_name"),
                            rs.getString("description"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("deadline", LocalDate.class),
                            rs.getString("status")
                    )
            );
        }

        Long orgId = tenantAccessService.requireOrganizationId(actor);
        return jdbcTemplate.query(
                baseSql + """
                        WHERE NULLIF(regexp_replace(COALESCE(p.organization_id::text, ''), '[^0-9]', '', 'g'), '')::BIGINT = ?
                        ORDER BY p.id DESC
                        """,
                (rs, rowNum) -> toProjectResponseDTO(
                        rs.getLong("id"),
                        rs.getString("project_name"),
                        rs.getString("description"),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("deadline", LocalDate.class),
                        rs.getString("status")
                ),
                orgId
        );
    }

    @Override
    public List<ProjectResponseDTO> getAssignedProjectResponses(Employee actor, Long employeeId) {
        String sql = """
                SELECT DISTINCT p.id, p.project_name, p.description, p.start_date, p.deadline, p.status
                FROM project p
                JOIN employee_projects ep ON ep.project_id = p.id
                WHERE ep.employee_id = ?
                ORDER BY p.id DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> toProjectResponseDTO(
                        rs.getLong("id"),
                        rs.getString("project_name"),
                        rs.getString("description"),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("deadline", LocalDate.class),
                        rs.getString("status")
                ),
                employeeId
        );
    }

    @Override
    public Optional<Project> getProjectById(Employee actor, Long id) {
        if (tenantAccessService.isMainAdmin(actor)) {
            return projectRepository.findById(id);
        }
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        return projectRepository.findByIdAndOrganization_Id(id, orgId);
    }

    @Override
    public Project updateProject(Employee actor, Long id, ProjectRequestDTO project) {
        tenantAccessService.assertCanManageEmployees(actor);
        Project existingProject = getProjectById(actor, id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        List<Employee> resolvedEmployees = applyProjectPayloadPrepare(actor, existingProject, project);
        Project saved = projectRepository.save(existingProject);
        syncProjectEmployeesOnOwnerSide(saved, resolvedEmployees);
        return projectRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public void deleteProject(Employee actor, Long id) {
        tenantAccessService.assertCanManageEmployees(actor);
        Project project = getProjectById(actor, id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        projectRepository.delete(project);
    }

    /**
     * Validates payload, resolves employees, and applies scalar fields to the project.
     * Employee–project links are persisted separately via {@link #syncProjectEmployeesOnOwnerSide}
     * because {@code Employee.projects} owns the {@code employee_projects} join table.
     */
    private List<Employee> applyProjectPayloadPrepare(Employee actor, Project entity, ProjectRequestDTO payload) {
        if (payload.getProjectName() == null || payload.getProjectName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Project name is required");
        }
        if (payload.getStartDate() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Project start date is required");
        }
        if (payload.getDeadline() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Project deadline is required");
        }
        if (payload.getDeadline().isBefore(payload.getStartDate())) {
            throw new ResponseStatusException(BAD_REQUEST, "Project deadline cannot be before start date");
        }
        if (payload.getDescription() != null && payload.getDescription().trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Project description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters"
            );
        }

        Organization organization = resolveProjectOrganization(actor);
        List<Employee> employees = payload.getEmployeeIds() == null || payload.getEmployeeIds().isEmpty()
                ? List.of()
                : userRepository.findAllById(payload.getEmployeeIds());

        if (payload.getEmployeeIds() != null && employees.size() != payload.getEmployeeIds().size()) {
            throw new ResponseStatusException(BAD_REQUEST, "One or more employees were not found");
        }
        Long orgId = organization.getId();
        boolean crossOrgAssignment = employees.stream().anyMatch(employee ->
                employee.getOrganization() == null || !Objects.equals(employee.getOrganization().getId(), orgId)
        );
        if (crossOrgAssignment) {
            throw new ResponseStatusException(BAD_REQUEST, "Employees must belong to the same organization as the project");
        }

        entity.setProjectName(payload.getProjectName().trim());
        entity.setDescription(payload.getDescription() == null ? "" : payload.getDescription().trim());
        entity.setStartDate(payload.getStartDate());
        entity.setDeadline(payload.getDeadline());
        entity.setStatus(normalizeProjectStatus(payload.getStatus()));
        entity.setOrganization(organization);
        return employees;
    }

    /**
     * Rewrites {@code employee_projects} for this project. JPA ignores updates made only on
     * {@link Project#getEmployees()} because {@code Employee.projects} owns the join table
     * ({@code mappedBy = "projects"}). Direct JDBC matches {@link #loadEmployeeIds} and avoids
     * stale in-memory collections on {@link Employee}.
     */
    private void syncProjectEmployeesOnOwnerSide(Project project, List<Employee> newAssignees) {
        Long projectId = project.getId();
        if (projectId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Project must be saved before assigning employees");
        }
        jdbcTemplate.update("DELETE FROM employee_projects WHERE project_id = ?", projectId);
        Set<Long> inserted = new HashSet<>();
        for (Employee assignee : newAssignees) {
            Long employeeId = assignee.getId();
            if (employeeId == null || !inserted.add(employeeId)) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO employee_projects (employee_id, project_id) VALUES (?, ?)",
                    employeeId,
                    projectId
            );
        }
    }

    private Organization resolveProjectOrganization(Employee actor) {
        if (tenantAccessService.isMainAdmin(actor)) {
            if (actor.getOrganization() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Main admin project creation requires an organization context");
            }
            return actor.getOrganization();
        }
        Long orgId = tenantAccessService.requireOrganizationId(actor);
        return actor.getOrganization() != null && Objects.equals(actor.getOrganization().getId(), orgId)
                ? actor.getOrganization()
                : userRepository.findById(actor.getId())
                    .map(Employee::getOrganization)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Organization not found"));
    }

    private String normalizeProjectStatus(String status) {
        String normalized = status == null ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("COMPLETED")) {
            throw new ResponseStatusException(BAD_REQUEST, "Project status must be ACTIVE or COMPLETED");
        }
        return normalized;
    }

    private ProjectResponseDTO toProjectResponseDTO(
            Long id,
            String projectName,
            String description,
            LocalDate startDate,
            LocalDate deadline,
            String status
    ) {
        return new ProjectResponseDTO(
                id,
                projectName,
                description,
                startDate,
                deadline,
                status,
                loadEmployeeIds(id)
        );
    }

    private List<Long> loadEmployeeIds(Long projectId) {
        return jdbcTemplate.query(
                "SELECT employee_id FROM employee_projects WHERE project_id = ? ORDER BY employee_id",
                (rs, rowNum) -> rs.getLong(1),
                projectId
        );
    }
}
