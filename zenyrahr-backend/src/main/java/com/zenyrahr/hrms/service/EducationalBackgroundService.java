package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.EducationalBackground;

import java.util.List;
import java.util.Optional;

public interface EducationalBackgroundService {

    EducationalBackground createEducationalBackground(EducationalBackground educationalBackground);

    Optional<EducationalBackground> getEducationalBackgroundById(Long id);

    List<EducationalBackground> getAllEducationalBackgrounds();

    EducationalBackground updateEducationalBackground(Long id, EducationalBackground educationalBackground);

    void deleteEducationalBackground(Long id);
}
