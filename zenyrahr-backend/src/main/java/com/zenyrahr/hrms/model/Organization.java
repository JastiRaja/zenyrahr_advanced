package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "organization")
@Data
public class Organization extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @Column(nullable = false)
    private Integer maxActiveUsers;

    @Column(nullable = false)
    private Boolean timesheetEnabled;

    @Column(nullable = false)
    private Boolean employeeManagementEnabled;

    @Column(nullable = false)
    private Boolean selfServiceEnabled;

    @Column(nullable = false)
    private Boolean attendanceEnabled;

    @Column(nullable = false)
    private Boolean leaveManagementEnabled;

    @Column(nullable = false)
    private Boolean holidayManagementEnabled;

    @Column(nullable = false)
    private Boolean payrollEnabled;

    @Column(nullable = false)
    private Boolean travelEnabled;

    @Column(nullable = false)
    private Boolean expenseEnabled;

    @Column(nullable = false, length = 32, columnDefinition = "varchar(32) default 'EMP'")
    private String employeeCodePrefix;

    @Column(nullable = false, columnDefinition = "integer default 4")
    private Integer employeeCodePadding;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private Integer nextEmployeeCodeNumber;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean allowManualEmployeeCodeOverride;
}
