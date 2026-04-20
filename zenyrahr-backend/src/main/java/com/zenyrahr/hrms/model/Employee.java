package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.zenyrahr.hrms.Timesheet.Project;
import com.zenyrahr.hrms.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Entity
@Table(name = "employee")
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 5)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column(nullable = false)
    private String role;

    private String email;

    private String position;

    private String department;

    private LocalDate joinDate;

    @Convert(converter = EncryptedStringConverter.class)
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    private String address;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private boolean firstLogin = true;

    @JsonManagedReference
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> education;

    @JsonManagedReference
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experience;

    @ElementCollection
    private List<String> skills;

    @ElementCollection
    private List<String> interests;

    @JsonManagedReference
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FamilyDetails> familyDetails;

    @JsonManagedReference
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalRecord> medicalRecords;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("leaveBalance-reference")
    private List<EmployeeLeaveBalance> leaveBalances;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Documents documents;

    @PrePersist
    public void prePersist() {
        if (this.joinDate == null) {
            this.joinDate = LocalDate.now();
        }
    }

    @Column(nullable = false)
    private String workLocation;

    @Column(name = "temporary_code")
    private String temporaryCode;

    @ManyToMany
    @JoinTable(
            name = "employee_projects",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @JsonIgnore
    private List<Project> projects;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee reportingManager;

    /** Populated for manager team directory responses only; not persisted. */
    @Transient
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String todayCheckInTime;

    /** Populated for manager team directory responses only; not persisted. */
    @Transient
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String todayCheckOutTime;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "description", insertable = false, updatable = false, length = 255)
    private String description;

    @Column(length = 50)
    private String jobType;

    @Column(length = 50)
    private String employeeStatus;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String maritalStatus;

    @Column(length = 50)
    private String nationality;

    @Column(length = 15)
    private Long alternateContactNumber;

    @Column(length = 100)
    private String personalEmailAddress;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String residentialAddress;

    @Column(length = 100)
    private String emergencyContactName;

    @Column(length = 50)
    private String emergencyContactRelation;

    @Column(length = 15)
    private String emergencyContactNumber;

    /**
     * Employee-controlled privacy flag.
     * If false, HR/ORG_ADMIN should not see emergency contact details.
     */
    private Boolean allowEmergencyContactVisibilityToHr = Boolean.FALSE;

    public Employee() {
        super();
    }

    public String getName() {
        return this.firstName + " " + this.lastName;
    }
}
