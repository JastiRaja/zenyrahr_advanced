package com.zenyrahr.hrms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceDTO {
    private Long employeeId;
    private String date;
    private String status;
    private String remarks;
    private String checkInTime;
    private String checkOutTime;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private String checkInLocationLabel;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private String checkOutLocationLabel;
} 