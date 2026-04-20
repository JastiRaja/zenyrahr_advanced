package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Timesheet.Project;
import com.zenyrahr.hrms.dto.EmployeeDTO;
import com.zenyrahr.hrms.model.Employee;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserDetailsService {

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    Employee saveEmployee(Employee employee);

    Optional<Employee> getEmployeeById(Long id);

    void deleteEmployee(Long id);

    // Update the employee and their associated entities
    Employee updateEmployee(Long id, Employee updatedEmployee);

    List<Employee> getAllUsers();

    List<Employee> getAllEmployee();

    Employee assignReportingManager(Long employeeId, Long managerId);

    Employee assignProjectsToEmployee(Long employeeId, List<Long> projectIds);

    List<Project> getAssignedProjects(Long employeeId);

    // In UserDetailsService interface
    Employee removeReportingManager(Long employeeId);

    Employee createEmployee(EmployeeDTO employeeDTO);

    Employee deactivateEmployee(Long id);

    Employee reactivateEmployee(Long id);
}
