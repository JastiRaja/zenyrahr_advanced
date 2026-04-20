package com.zenyrahr.hrms.Timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Timesheet, Long> {
}
