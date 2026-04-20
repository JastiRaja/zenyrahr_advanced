package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.dto.AttendanceDTO;
import com.zenyrahr.hrms.dto.PunchRequestDTO;
import com.zenyrahr.hrms.model.Attendance;
import com.zenyrahr.hrms.Repository.AttendanceRepository;
import com.zenyrahr.hrms.Repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.zenyrahr.hrms.dto.AttendanceStatsDTO;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class AttendanceService {

    /** Employee has punched in but not yet out; excluded from attendance stats totals. */
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";

    private static final long FULL_DAY_MINUTES = 8L * 60L;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    public void saveBatchAttendance(List<AttendanceDTO> attendanceList) {
        for (AttendanceDTO dto : attendanceList) {
            LocalDate localDate = LocalDate.parse(dto.getDate());
            Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), localDate)
                .orElseGet(() -> {
                    Attendance newAtt = new Attendance();
                    newAtt.setEmployee(employeeRepository.findById(dto.getEmployeeId()).orElseThrow());
                    newAtt.setDate(localDate);
                    return newAtt;
                });
            attendance.setStatus(dto.getStatus());
            attendance.setRemarks(dto.getRemarks());
            attendanceRepository.save(attendance);
        }
    }

    public AttendanceDTO punchIn(Long employeeId, PunchRequestDTO req) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElseGet(() -> {
                Attendance a = new Attendance();
                a.setEmployee(employeeRepository.findById(employeeId).orElseThrow());
                a.setDate(today);
                return a;
            });

        String st = attendance.getStatus();
        if (st != null && (st.equalsIgnoreCase("LEAVE") || st.equalsIgnoreCase("HOLIDAY"))) {
            throw new IllegalStateException("Attendance is marked as leave or holiday for today.");
        }
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null) {
            throw new IllegalStateException("You have already punched in today. Punch out first tomorrow is a new day.");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("Today's attendance is already complete.");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckInTime(now);
        attendance.setCheckInLatitude(req != null ? req.getLatitude() : null);
        attendance.setCheckInLongitude(req != null ? req.getLongitude() : null);
        attendance.setCheckInLocationLabel(req != null ? req.getLocationLabel() : null);
        attendance.setStatus(STATUS_CHECKED_IN);
        attendanceRepository.save(attendance);
        return toDto(attendance);
    }

    public AttendanceDTO punchOut(Long employeeId, PunchRequestDTO req) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElseThrow(() -> new IllegalStateException("No punch-in found for today."));

        if (attendance.getCheckInTime() == null) {
            throw new IllegalStateException("No punch-in found for today.");
        }
        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("You have already punched out today.");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);
        attendance.setCheckOutLatitude(req != null ? req.getLatitude() : null);
        attendance.setCheckOutLongitude(req != null ? req.getLongitude() : null);
        attendance.setCheckOutLocationLabel(req != null ? req.getLocationLabel() : null);

        Duration worked = Duration.between(attendance.getCheckInTime(), now);
        long minutes = worked.toMinutes();
        if (minutes >= FULL_DAY_MINUTES) {
            attendance.setStatus("PRESENT");
        } else {
            attendance.setStatus("HALF_DAY");
        }
        if (attendance.getRemarks() == null || attendance.getRemarks().isBlank()) {
            attendance.setRemarks("Self punch: " + minutes / 60 + "h " + minutes % 60 + "m between punch in and punch out.");
        }

        attendanceRepository.save(attendance);
        return toDto(attendance);
    }

    public AttendanceDTO getTodayAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.now())
            .map(this::toDto)
            .orElse(null);
    }

    private AttendanceDTO toDto(Attendance att) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(att.getEmployee().getId());
        dto.setDate(att.getDate().toString());
        dto.setStatus(att.getStatus());
        dto.setRemarks(att.getRemarks());
        if (att.getCheckInTime() != null) {
            dto.setCheckInTime(att.getCheckInTime().toString());
        }
        if (att.getCheckOutTime() != null) {
            dto.setCheckOutTime(att.getCheckOutTime().toString());
        }
        dto.setCheckInLatitude(att.getCheckInLatitude());
        dto.setCheckInLongitude(att.getCheckInLongitude());
        dto.setCheckInLocationLabel(att.getCheckInLocationLabel());
        dto.setCheckOutLatitude(att.getCheckOutLatitude());
        dto.setCheckOutLongitude(att.getCheckOutLongitude());
        dto.setCheckOutLocationLabel(att.getCheckOutLocationLabel());
        return dto;
    }

    private boolean countsTowardStats(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        if (STATUS_CHECKED_IN.equalsIgnoreCase(status)) {
            return false;
        }
        return true;
    }

    public List<AttendanceDTO> getAttendanceForEmployeeAndMonth(Long employeeId, String month, String year) {
        int monthInt = Integer.parseInt(month);
        int yearInt = Integer.parseInt(year);
        java.time.LocalDate start = java.time.LocalDate.of(yearInt, monthInt, 1);
        java.time.LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Attendance> records = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
        List<AttendanceDTO> dtos = new ArrayList<>();
        for (Attendance att : records) {
            dtos.add(toDto(att));
        }
        return dtos;
    }

    public List<AttendanceStatsDTO> getMonthlyAttendanceStats() {
        List<Attendance> all = attendanceRepository.findAll();
        Map<String, AttendanceStatsDTO> statsMap = new HashMap<>();
        for (Attendance att : all) {
            if (!countsTowardStats(att.getStatus())) {
                continue;
            }
            String month = att.getDate().getYear() + "-" + String.format("%02d", att.getDate().getMonthValue());
            AttendanceStatsDTO stats = statsMap.getOrDefault(month, new AttendanceStatsDTO());
            stats.setMonth(month);
            stats.setTotal(stats.getTotal() + 1);
            if ("PRESENT".equalsIgnoreCase(att.getStatus())) {
                stats.setPresentCount(stats.getPresentCount() + 1);
            } else if ("ABSENT".equalsIgnoreCase(att.getStatus())) {
                stats.setAbsentCount(stats.getAbsentCount() + 1);
            } else if ("HALF_DAY".equalsIgnoreCase(att.getStatus())) {
                stats.setHalfDayCount(stats.getHalfDayCount() + 1);
            }
            statsMap.put(month, stats);
        }
        return statsMap.values().stream()
            .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))
            .collect(Collectors.toList());
    }

    public List<AttendanceStatsDTO> getAttendanceStats(String period, String department, Long organizationId, Long employeeId) {
        List<Attendance> all;
        if (employeeId != null) {
            all = attendanceRepository.findByEmployeeId(employeeId);
        } else if (organizationId != null) {
            all = attendanceRepository.findByEmployee_Organization_Id(organizationId);
        } else {
            all = attendanceRepository.findAll();
        }
        // Filter by department if provided
        if (department != null && !department.isEmpty()) {
            all = all.stream()
                .filter(att -> att.getEmployee().getDepartment() != null && att.getEmployee().getDepartment().equalsIgnoreCase(department))
                .collect(Collectors.toList());
        }
        Map<String, AttendanceStatsDTO> statsMap = new HashMap<>();
        for (Attendance att : all) {
            if (!countsTowardStats(att.getStatus())) {
                continue;
            }
            String key;
            if ("weekly".equalsIgnoreCase(period)) {
                java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
                int week = att.getDate().get(weekFields.weekOfWeekBasedYear());
                int year = att.getDate().getYear();
                key = year + "-W" + String.format("%02d", week);
            } else {
                key = att.getDate().getYear() + "-" + String.format("%02d", att.getDate().getMonthValue());
            }
            AttendanceStatsDTO stats = statsMap.getOrDefault(key, new AttendanceStatsDTO());
            stats.setMonth(key); // You may want to rename this to 'period' in DTO
            stats.setTotal(stats.getTotal() + 1);
            if ("PRESENT".equalsIgnoreCase(att.getStatus())) {
                stats.setPresentCount(stats.getPresentCount() + 1);
            } else if ("ABSENT".equalsIgnoreCase(att.getStatus())) {
                stats.setAbsentCount(stats.getAbsentCount() + 1);
            } else if ("HALF_DAY".equalsIgnoreCase(att.getStatus())) {
                stats.setHalfDayCount(stats.getHalfDayCount() + 1);
            }
            statsMap.put(key, stats);
        }
        return statsMap.values().stream()
            .sorted(Comparator.comparing(AttendanceStatsDTO::getMonth))
            .collect(Collectors.toList());
    }

    public long getTodayPunchInCount(Long organizationId) {
        LocalDate today = LocalDate.now();
        if (organizationId == null) {
            return attendanceRepository.countByDateAndCheckInTimeIsNotNull(today);
        }
        return attendanceRepository.countByDateAndEmployee_Organization_IdAndCheckInTimeIsNotNull(today, organizationId);
    }
} 