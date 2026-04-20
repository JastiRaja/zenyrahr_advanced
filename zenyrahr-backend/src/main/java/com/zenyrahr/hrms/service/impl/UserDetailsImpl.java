package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.EducationRepository;
import com.zenyrahr.hrms.Repository.ExperienceRepository;
import com.zenyrahr.hrms.Repository.FamilyDetailsRepository;
import com.zenyrahr.hrms.Repository.MedicalRecordRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.Timesheet.Project;
import com.zenyrahr.hrms.Timesheet.ProjectRepository;
import com.zenyrahr.hrms.dto.EmployeeDTO;
import com.zenyrahr.hrms.model.Education;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Experience;
import com.zenyrahr.hrms.model.FamilyDetails;
import com.zenyrahr.hrms.model.MedicalRecord;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.UserDetailsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class UserDetailsImpl implements UserDetailsService {

    private final UserRepository employeeRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final FamilyDetailsRepository familyDetailRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final SequenceService sequenceService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee user = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean isActive = Boolean.TRUE.equals(user.getActive());
        boolean accountNonLocked = !user.isLocked() && isActive;
        String rawRole = user.getRole() == null ? "" : user.getRole();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(rawRole));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rawRole.toUpperCase()));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isActive,
                true,
                true,
                accountNonLocked,
                authorities
        );
    }


    @Override
    public Employee saveEmployee(Employee employee) {
        String rawPassword = employee.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateRandomPassword();
        }

        employee.setPassword(passwordEncoder.encode(rawPassword));
        employee.setFirstLogin(true);
        if (employee.getCode() == null || employee.getCode().isBlank()) {
            employee.setCode(sequenceService.getNextCode("USER"));
        }
        employee.setEmployeeStatus("Active");
        employee.setOrganization(resolveOrganization(employee.getOrganization()));


        // Link all child entities to the employee
        linkChildEntitiesToEmployee(employee);

        return employeeRepository.save(employee);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    @Override
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Remove manager linkage from subordinates to avoid self-reference FK violations.
        List<Employee> subordinates = employeeRepository.findByReportingManager_Id(id);
        for (Employee subordinate : subordinates) {
            subordinate.setReportingManager(null);
        }
        if (!subordinates.isEmpty()) {
            employeeRepository.saveAll(subordinates);
        }

        // Clear many-to-many join table entries before delete.
        if (employee.getProjects() != null) {
            employee.getProjects().clear();
        }
        employeeRepository.save(employee);
        employeeRepository.delete(employee);
    }

    @Override
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Update basic employee fields
        updateEmployeeFields(existingEmployee, updatedEmployee);

        // Update related entities
        updateRelatedEntities(updatedEmployee.getEducation(), existingEmployee.getEducation(), educationRepository, existingEmployee);
        updateRelatedEntities(updatedEmployee.getExperience(), existingEmployee.getExperience(), experienceRepository, existingEmployee);
        updateRelatedEntities(updatedEmployee.getFamilyDetails(), existingEmployee.getFamilyDetails(), familyDetailRepository, existingEmployee);
        updateRelatedEntities(updatedEmployee.getMedicalRecords(), existingEmployee.getMedicalRecords(), medicalRecordRepository, existingEmployee);
//        updateRelatedEntities(updatedEmployee.getSkillsAndInterests(),existingEmployee.getSkillsAndInterests(),skillsAndInterestsRepository,existingEmployee);
        return employeeRepository.save(existingEmployee);
    }

    @Override
    public List<Employee> getAllUsers() {
        return employeeRepository.findAll();
    }

    @Override
    public List<Employee> getAllEmployee() {
        return employeeRepository.findAll();
    }

    // In UserDetailsImpl.java (service implementation)
    @Override
    @Transactional
    public Employee assignReportingManager(Long employeeId, Long managerId) {
        // Update manager (existing implementation)
        if (employeeId.equals(managerId)) {
            throw new IllegalArgumentException("Employee cannot be their own manager");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        employee.setReportingManager(manager);
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public Employee removeReportingManager(Long employeeId) {
        // New method to remove manager
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setReportingManager(null);
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public Employee assignProjectsToEmployee(Long employeeId, List<Long> projectIds) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<Project> projects = projectIds == null || projectIds.isEmpty()
                ? List.of()
                : projectRepository.findAllById(projectIds);

        if (projectIds != null && projects.size() != projectIds.size()) {
            throw new RuntimeException("One or more projects were not found");
        }

        employee.setProjects(new ArrayList<>(projects));
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public List<Project> getAssignedProjects(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        if (employee.getProjects() == null) {
            return List.of();
        }
        return new ArrayList<>(employee.getProjects());
    }

    @Override
    @Transactional
    public Employee deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employee.setActive(false);
        employee.setLocked(true);
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public Employee reactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employee.setActive(true);
        employee.setLocked(false);
        if (employee.getEmployeeStatus() == null || employee.getEmployeeStatus().isBlank()) {
            employee.setEmployeeStatus("Active");
        }
        return employeeRepository.save(employee);
    }

    private void linkChildEntitiesToEmployee(Employee employee) {
        if (employee.getEducation() != null) {
            employee.getEducation().forEach(education -> education.setEmployee(employee));
        }

        if (employee.getExperience() != null) {
            employee.getExperience().forEach(experience -> experience.setEmployee(employee));
        }

        if (employee.getFamilyDetails() != null) {
            employee.getFamilyDetails().forEach(familyDetail -> familyDetail.setEmployee(employee));
        }

        if (employee.getMedicalRecords() != null) {
            employee.getMedicalRecords().forEach(medicalRecord -> medicalRecord.setEmployee(employee));
        }
    }

    private void updateEmployeeFields(Employee existingEmployee, Employee updatedEmployee) {
        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setRole(updatedEmployee.getRole());
        existingEmployee.setPosition(updatedEmployee.getPosition());
        existingEmployee.setDepartment(updatedEmployee.getDepartment());
        existingEmployee.setWorkLocation(updatedEmployee.getWorkLocation());
        existingEmployee.setPhone(updatedEmployee.getPhone());
        existingEmployee.setAddress(updatedEmployee.getAddress());
        existingEmployee.setActive(updatedEmployee.getActive());
        existingEmployee.setSkills(updatedEmployee.getSkills());
        existingEmployee.setInterests(updatedEmployee.getInterests());
        existingEmployee.setEmergencyContactName(updatedEmployee.getEmergencyContactName());
        existingEmployee.setEmergencyContactRelation(updatedEmployee.getEmergencyContactRelation());
        existingEmployee.setEmergencyContactNumber(updatedEmployee.getEmergencyContactNumber());
        existingEmployee.setAlternateContactNumber(updatedEmployee.getAlternateContactNumber());
        existingEmployee.setAllowEmergencyContactVisibilityToHr(
                Objects.equals(updatedEmployee.getAllowEmergencyContactVisibilityToHr(), Boolean.TRUE)
        );
        existingEmployee.setOrganization(resolveOrganization(updatedEmployee.getOrganization()));
    }

    private Organization resolveOrganization(Organization organization) {
        if (organization == null) {
            return null;
        }
        if (organization.getId() == null) {
            return null;
        }
        return organizationRepository.findById(organization.getId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    private <T> void updateRelatedEntities(List<T> updatedEntities, List<T> existingEntities, JpaRepository<T, Long> repository, Employee employee) {
        if (updatedEntities != null) {
            // Add or update entities
            for (T updatedEntity : updatedEntities) {
                Long id = getId(updatedEntity);
                if (id == null) {
                    linkToEmployee(updatedEntity, employee);
                    repository.save(updatedEntity);
                } else {
                    T existingEntity = repository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Entity not found"));
                    copyFields(updatedEntity, existingEntity);
                    repository.save(existingEntity);
                }
            }

            // Remove entities not in updated list
            List<Long> updatedIds = updatedEntities.stream().map(this::getId).toList();
            existingEntities.removeIf(existingEntity -> !updatedIds.contains(getId(existingEntity)));
        }
    }

    private <T> void linkToEmployee(T entity, Employee employee) {
        if (entity instanceof Education education) {
            education.setEmployee(employee);
        } else if (entity instanceof Experience experience) {
            experience.setEmployee(employee);
        } else if (entity instanceof FamilyDetails familyDetail) {
            familyDetail.setEmployee(employee);
        } else if (entity instanceof MedicalRecord medicalRecord) {
            medicalRecord.setEmployee(employee);
        }
    }

    private <T> Long getId(T entity) {
        if (entity instanceof Education education) {
            return education.getId();
        } else if (entity instanceof Experience experience) {
            return experience.getId();
        } else if (entity instanceof FamilyDetails familyDetail) {
            return familyDetail.getId();
        } else if (entity instanceof MedicalRecord medicalRecord) {
            return medicalRecord.getId();
        }
        return null;
    }

    private <T> void copyFields(T source, T target) {
        if (source instanceof Education updated && target instanceof Education existing) {
            existing.setDegree(updated.getDegree());
            existing.setInstitution(updated.getInstitution());
            existing.setYear(updated.getYear());
            existing.setField(updated.getField());
        } else if (source instanceof Experience updated && target instanceof Experience existing) {
            existing.setCompany(updated.getCompany());
            existing.setPosition(updated.getPosition());
            existing.setStartDate(updated.getStartDate());
            existing.setEndDate(updated.getEndDate());
            existing.setDescription(updated.getDescription());
        } else if (source instanceof FamilyDetails updated && target instanceof FamilyDetails existing) {
            existing.setName(updated.getName());
            existing.setRelationship(updated.getRelationship());
            existing.setContact(updated.getContact());
        } else if (source instanceof MedicalRecord updated && target instanceof MedicalRecord existing) {
            existing.setCondition(updated.getCondition());
            existing.setDate(updated.getDate());
            existing.setDetails(updated.getDetails());
        }
    }

    public Employee createEmployee(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setEmail(employeeDTO.getEmail());
        return employeeRepository.save(employee);
    }

    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Employee employee = getEmployee(id);
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setEmail(employeeDTO.getEmail());
        return employeeRepository.save(employee);
    }


}
