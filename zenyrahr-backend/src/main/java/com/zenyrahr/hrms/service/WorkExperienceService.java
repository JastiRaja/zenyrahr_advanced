package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.WorkExperience;

import java.util.List;
import java.util.Optional;

public interface WorkExperienceService {

    WorkExperience createWorkExperience(WorkExperience workExperience);

    Optional<WorkExperience> getWorkExperienceById(Long id);

    List<WorkExperience> getAllWorkExperience();

    WorkExperience updateWorkExperience(Long id, WorkExperience workExperience);

    void deleteWorkExperience(Long id);
}
