package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.WorkShift;

import java.util.List;
import java.util.Optional;

public interface WorkShiftDataService {

    WorkShift createWorkShift(WorkShift workShift);

    Optional<WorkShift> getWorkShiftById(Long id);

    List<WorkShift> getAllWorkShifts();

    WorkShift updateWorkShift(Long id, WorkShift workShift);

    void deleteWorkShift(Long id);
}
