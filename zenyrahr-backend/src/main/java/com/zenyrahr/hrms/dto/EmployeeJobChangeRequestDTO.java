package com.zenyrahr.hrms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeJobChangeRequestDTO {
    private String changeType;
    private String role;
    private String position;
    private String department;
    private String workLocation;
    private Long managerId;
    private boolean clearManager;
    private LocalDate effectiveDate;
    private String reason;
}
