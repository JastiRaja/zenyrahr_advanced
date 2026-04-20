package com.zenyrahr.hrms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeJobHistoryDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long changedById;
    private String changedByName;
    private String changeType;
    private String oldRole;
    private String newRole;
    private String oldPosition;
    private String newPosition;
    private String oldDepartment;
    private String newDepartment;
    private String oldWorkLocation;
    private String newWorkLocation;
    private Long oldManagerId;
    private String oldManagerName;
    private Long newManagerId;
    private String newManagerName;
    private LocalDate effectiveDate;
    private String reason;
    private LocalDateTime changedAt;
}
