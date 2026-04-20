package com.zenyrahr.hrms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceStatsDTO {
    private String month; // e.g., "2024-05"
    private int presentCount;
    private int absentCount;
    private int halfDayCount;
    private int total;
} 