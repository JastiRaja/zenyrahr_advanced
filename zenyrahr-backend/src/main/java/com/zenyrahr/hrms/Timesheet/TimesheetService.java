package com.zenyrahr.hrms.Timesheet;

import com.zenyrahr.hrms.model.Employee;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
public interface TimesheetService {
    
    Timesheet createTimesheetcreate(Employee employee, LocalDateTime startTime, LocalDateTime endTime, String taskDescription, String comments);

    Optional<Timesheet> getTimesheetById(Long id);

    List<Timesheet> getAllTimesheets(Employee actor);

    List<Timesheet> getTimesheetsBetweenDates(LocalDate startDate, LocalDate endDate);
//    Timesheet updateTimesheet(Long id, Timesheet timesheet);

    void deleteTimesheet(Long id);

    List<Timesheet> getTimesheetsByEmployee(Employee actor, Long employeeId);

    Timesheet updateTimesheet(Long id, Timesheet timesheet);

    Timesheet withdrawTimesheet(Long id);


//    Timesheet approveTimesheet(Long id);

    Timesheet createTimesheet(Timesheet timesheet);

    Timesheet approveTimesheet(Long id, Long approverId, String requiredComments, int requiredLevels);

    Timesheet rejectTimesheet(Long id, Long employeeId, String requiredComments);

    List<Timesheet> getTimesheetsByStatus(TimeSheetStatus status);
    List<Timesheet> getTimesheetsByStatusAndEmployeeId(TimeSheetStatus status, Long employeeId);

}

 