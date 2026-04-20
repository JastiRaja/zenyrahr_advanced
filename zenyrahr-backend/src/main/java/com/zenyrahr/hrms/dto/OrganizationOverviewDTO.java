package com.zenyrahr.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationOverviewDTO {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String logoUrl;
    private Boolean active;
    private Long userCount;
    private Long activeUserCount;
    private Long activeProjectCount;
    private Integer maxActiveUsers;
    private Boolean timesheetEnabled;
    private Boolean employeeManagementEnabled;
    private Boolean selfServiceEnabled;
    private Boolean attendanceEnabled;
    private Boolean leaveManagementEnabled;
    private Boolean holidayManagementEnabled;
    private Boolean payrollEnabled;
    private Boolean travelEnabled;
    private Boolean expenseEnabled;
}
