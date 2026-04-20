package com.zenyrahr.hrms.Timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByEmployeeId(Long employeeId);

    Optional<Timesheet> findByIdAndEmployeeId(Long id, Long employeeId);

    Optional<Timesheet> findById(Long id);

    @Query("SELECT t FROM Timesheet t WHERE t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findTimesheetsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Timesheet> findByStatus(TimeSheetStatus status);

    List<Timesheet> findByStatusAndEmployeeId(TimeSheetStatus status, Long employeeId);

}
