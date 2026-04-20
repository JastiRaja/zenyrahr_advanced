package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.PayrollGeneration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    /** Platform operator (multi-tenant). Must not collide with organization-scoped role names like {@code org_admin}. */
    public static final String MAIN_PLATFORM_ADMIN_ROLE = "zenyrahr_admin";

    private final UserRepository userRepository;

    public Employee requireCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authenticated user not found"));
    }

    public boolean isMainAdmin(Employee employee) {
        return employee != null && MAIN_PLATFORM_ADMIN_ROLE.equalsIgnoreCase(employee.getRole());
    }

    public boolean isOrgAdmin(Employee employee) {
        return employee != null && "org_admin".equalsIgnoreCase(employee.getRole());
    }

    public boolean isHr(Employee employee) {
        return employee != null && "hr".equalsIgnoreCase(employee.getRole());
    }

    public boolean isManager(Employee employee) {
        return employee != null && "manager".equalsIgnoreCase(employee.getRole());
    }

    public boolean isPrivilegedRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.toLowerCase(Locale.ROOT);
        return normalized.equals(MAIN_PLATFORM_ADMIN_ROLE) || normalized.equals("org_admin");
    }

    public boolean canManageEmployees(Employee employee) {
        if (employee == null || employee.getRole() == null) {
            return false;
        }
        String role = employee.getRole().toLowerCase(Locale.ROOT);
        return role.equals(MAIN_PLATFORM_ADMIN_ROLE) || role.equals("org_admin") || role.equals("hr");
    }

    public Long requireOrganizationId(Employee employee) {
        if (employee == null || employee.getOrganization() == null || employee.getOrganization().getId() == null) {
            throw new ResponseStatusException(FORBIDDEN, "User is not assigned to an organization");
        }
        return employee.getOrganization().getId();
    }

    /**
     * Resolves which organization an API call is scoped to. Platform admins must pass {@code organizationId}
     * explicitly so list and mutation endpoints never default to a cross-tenant "global" view by accident.
     */
    public Long resolveOrganizationIdForScopedQuery(Employee actor, Long organizationIdQueryParam) {
        if (isMainAdmin(actor)) {
            if (organizationIdQueryParam == null) {
                throw new ResponseStatusException(BAD_REQUEST, "organizationId query parameter is required");
            }
            return organizationIdQueryParam;
        }
        Long orgId = requireOrganizationId(actor);
        if (organizationIdQueryParam != null && !Objects.equals(organizationIdQueryParam, orgId)) {
            throw new ResponseStatusException(FORBIDDEN, "organizationId does not match your organization");
        }
        return orgId;
    }

    public void assertPayrollGenerationAccessible(Employee actor, PayrollGeneration payrollGeneration) {
        if (actor == null || payrollGeneration == null) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }
        if (isMainAdmin(actor)) {
            return;
        }
        Long actorOrgId = requireOrganizationId(actor);
        if (payrollGeneration.getOrganizationId() == null || !Objects.equals(actorOrgId, payrollGeneration.getOrganizationId())) {
            throw new ResponseStatusException(FORBIDDEN, "Payroll run belongs to another organization");
        }
    }

    public void assertOrganizationActive(Employee employee) {
        if (isMainAdmin(employee)) {
            return;
        }
        if (employee == null || employee.getOrganization() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Organization assignment is required");
        }
        if (!Boolean.TRUE.equals(employee.getOrganization().getActive())) {
            throw new ResponseStatusException(FORBIDDEN, "ORGANIZATION_DISABLED");
        }
    }

    public void assertCanManageEmployees(Employee actor) {
        if (!canManageEmployees(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "You are not allowed to manage employees");
        }
    }

    public void assertCanAccessEmployee(Employee actor, Employee target) {
        if (target == null) {
            throw new ResponseStatusException(FORBIDDEN, "Employee not found");
        }
        if (isMainAdmin(actor)) {
            return;
        }
        Long actorOrgId = requireOrganizationId(actor);
        Long targetOrgId = target.getOrganization() != null ? target.getOrganization().getId() : null;
        if (!Objects.equals(actorOrgId, targetOrgId)) {
            throw new ResponseStatusException(FORBIDDEN, "Cross-organization access is not allowed");
        }
        if (isManager(actor)) {
            if (Objects.equals(actor.getId(), target.getId())) {
                return;
            }
            Employee reportingManager = target.getReportingManager();
            if (reportingManager == null || !Objects.equals(reportingManager.getId(), actor.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "Managers can only access their direct reports");
            }
        }
    }

    public void assertCanAccessEmployeeId(Employee actor, Long employeeId) {
        Employee target = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Employee not found"));
        assertCanAccessEmployee(actor, target);
    }

    public List<Employee> filterEmployeesByScope(Employee actor, List<Employee> employees) {
        if (isMainAdmin(actor)) {
            return employees;
        }
        Long actorOrgId = requireOrganizationId(actor);
        return employees.stream()
                .filter(e -> e.getOrganization() != null && Objects.equals(e.getOrganization().getId(), actorOrgId))
                .toList();
    }

    /** Employees who report to this manager, scoped to the manager's organization. */
    public List<Employee> listDirectReportsForManager(Employee actor) {
        if (!isManager(actor)) {
            return List.of();
        }
        Long orgId = requireOrganizationId(actor);
        return userRepository.findByReportingManager_Id(actor.getId()).stream()
                .filter(e -> e.getOrganization() != null && Objects.equals(e.getOrganization().getId(), orgId))
                .toList();
    }

    public Optional<Long> scopedOrganizationForNewEmployee(Employee actor, Employee incoming) {
        if (isMainAdmin(actor)) {
            return Optional.ofNullable(incoming.getOrganization()).map(o -> o.getId());
        }
        return Optional.of(requireOrganizationId(actor));
    }

    /**
     * Blocks org admins from creating users with {@code zenyrahr_admin}/{@code org_admin} roles, or from
     * promoting users to those roles. Self-service profile updates keep the same role and must be allowed.
     *
     * @param existingBeforeUpdate {@code null} for create/register; the persisted employee row for updates
     */
    public void assertOrgAdminDoesNotCreatePrivilegedUsers(Employee actor, Employee incoming, Employee existingBeforeUpdate) {
        if (actor == null || incoming == null) {
            return;
        }
        String role = incoming.getRole() == null ? "" : incoming.getRole().toLowerCase(Locale.ROOT);
        if (isOrgAdmin(actor) && isPrivilegedRole(role)) {
            if (existingBeforeUpdate == null) {
                throw new ResponseStatusException(FORBIDDEN, "Organization admin cannot create platform admin users");
            }
            String previous = existingBeforeUpdate.getRole() == null ? "" : existingBeforeUpdate.getRole().toLowerCase(Locale.ROOT);
            if (!role.equals(previous)) {
                throw new ResponseStatusException(FORBIDDEN, "Organization admin cannot create platform admin users");
            }
        }
        if (isHr(actor) && isPrivilegedRole(role)) {
            throw new ResponseStatusException(FORBIDDEN, "HR cannot create or assign platform admin roles");
        }
    }

    /** Create / register — no existing employee row. */
    public void assertOrgAdminDoesNotCreatePrivilegedUsers(Employee actor, Employee incoming) {
        assertOrgAdminDoesNotCreatePrivilegedUsers(actor, incoming, null);
    }

    public void assertHrCannotModifyPrivilegedUser(Employee actor, Employee target) {
        if (!isHr(actor) || target == null) {
            return;
        }
        String targetRole = target.getRole() == null ? "" : target.getRole().toLowerCase(Locale.ROOT);
        if (isPrivilegedRole(targetRole)) {
            throw new ResponseStatusException(FORBIDDEN, "HR cannot modify platform admin or organization admin accounts");
        }
    }
}
