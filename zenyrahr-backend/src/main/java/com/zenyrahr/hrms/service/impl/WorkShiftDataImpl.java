package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.WorkShift;
import com.zenyrahr.hrms.Repository.WorkShiftRepository;
import com.zenyrahr.hrms.service.WorkShiftDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkShiftDataImpl implements WorkShiftDataService {

    private final WorkShiftRepository workShiftRepository;
    private final SequenceService sequenceService;

    @Override
    public WorkShift createWorkShift(WorkShift workShift) {
        workShift.setCode(sequenceService.getNextCode("WORKSHIFT"));
        return workShiftRepository.save(workShift);
    }

    @Override
    public Optional<WorkShift> getWorkShiftById(Long id) {
        return workShiftRepository.findById(id);
    }

    @Override
    public List<WorkShift> getAllWorkShifts() {
        return workShiftRepository.findAll();
    }

    @Override
    public WorkShift updateWorkShift(Long id, WorkShift workShift) {
        if (workShiftRepository.existsById(id)) {
            workShift.setId(id);
            return workShiftRepository.save(workShift);
        } else {
            throw new RuntimeException("WorkShift not found");
        }
    }

    @Override
    public void deleteWorkShift(Long id) {
        workShiftRepository.deleteById(id);
    }
}
