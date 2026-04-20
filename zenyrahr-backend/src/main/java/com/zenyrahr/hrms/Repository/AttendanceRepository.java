package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<Attendance> findByEmployeeId(Long employeeId);
    List<Attendance> findByEmployee_Organization_Id(Long organizationId);
    long countByDateAndCheckInTimeIsNotNull(LocalDate date);
    long countByDateAndEmployee_Organization_IdAndCheckInTimeIsNotNull(LocalDate date, Long organizationId);
} 