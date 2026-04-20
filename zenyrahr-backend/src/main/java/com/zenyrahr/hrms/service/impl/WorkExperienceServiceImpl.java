package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.WorkExperience;
import com.zenyrahr.hrms.Repository.WorkExperienceRepository;
import com.zenyrahr.hrms.service.WorkExperienceService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkExperienceServiceImpl implements WorkExperienceService {

    private final WorkExperienceRepository workExperienceRepository;
    private final SequenceService sequenceService;

    @Override
    public WorkExperience createWorkExperience(WorkExperience workExperience) {
        // Set code using sequence service
        workExperience.setCode(sequenceService.getNextCode("WORKEXPERIENCE"));
        return workExperienceRepository.save(workExperience); // Save new work experience record
    }

    @Override
    public Optional<WorkExperience> getWorkExperienceById(Long id) {
        return workExperienceRepository.findById(id); // Find work experience by id
    }

    @Override
    public List<WorkExperience> getAllWorkExperience() {
        return workExperienceRepository.findAll(); // Fetch all work experience records
    }

    @Override
    public WorkExperience updateWorkExperience(Long id, WorkExperience workExperience) {
        if (workExperienceRepository.existsById(id)) {
            workExperience.setId(id); // Ensure the existing id is set before updating
            return workExperienceRepository.save(workExperience); // Save the updated work experience record
        } else {
            throw new RuntimeException("Work Experience not found");
        }
    }

    @Override
    public void deleteWorkExperience(Long id) {
        workExperienceRepository.deleteById(id); // Delete the work experience by id
    }
}
