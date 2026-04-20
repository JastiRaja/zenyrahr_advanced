package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.EducationalBackground;
import com.zenyrahr.hrms.Repository.EducationalBackgroundRepository;
import com.zenyrahr.hrms.service.EducationalBackgroundService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EducationalBackgroundServiceImpl implements EducationalBackgroundService {

    private final EducationalBackgroundRepository educationalBackgroundRepository;
    private final SequenceService sequenceService;

    @Override
    public EducationalBackground createEducationalBackground(EducationalBackground educationalBackground) {
        educationalBackground.setCode(sequenceService.getNextCode("EDUBACK"));
        return educationalBackgroundRepository.save(educationalBackground);
    }

    @Override
    public Optional<EducationalBackground> getEducationalBackgroundById(Long id) {
        return educationalBackgroundRepository.findById(id);
    }

    @Override
    public List<EducationalBackground> getAllEducationalBackgrounds() {
        return educationalBackgroundRepository.findAll();
    }

    @Override
    public EducationalBackground updateEducationalBackground(Long id, EducationalBackground educationalBackground) {
        if (educationalBackgroundRepository.existsById(id)) {
//            educationalBackground.setId(id);
            return educationalBackgroundRepository.save(educationalBackground);
        } else {
            throw new RuntimeException("Educational Background not found");
        }
    }

    @Override
    public void deleteEducationalBackground(Long id) {
        educationalBackgroundRepository.deleteById(id);
    }
}
