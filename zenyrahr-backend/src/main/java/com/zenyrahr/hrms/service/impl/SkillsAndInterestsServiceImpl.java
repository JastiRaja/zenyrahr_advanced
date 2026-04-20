//package com.talvox.hrms.service.impl;
//
//import com.talvox.hrms.model.SkillsAndInterests;
//import com.talvox.hrms.Repository.SkillsAndInterestsRepository;
//import com.talvox.hrms.service.SkillsAndInterestsService;
//import com.talvox.hrms.service.SequenceService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class SkillsAndInterestsServiceImpl implements SkillsAndInterestsService {
//
//    private final SkillsAndInterestsRepository skillsAndInterestsRepository;
//    private final SequenceService sequenceService;
//
//    @Override
//    public SkillsAndInterests createSkillsAndInterests(SkillsAndInterests skillsAndInterests) {
////        skillsAndInterests.setCode(sequenceService.getNextCode("SKILLSINT"));
//        return skillsAndInterestsRepository.save(skillsAndInterests);
//    }
//
//    @Override
//    public Optional<SkillsAndInterests> getSkillsAndInterestsById(Long id) {
//        return skillsAndInterestsRepository.findById(id);
//    }
//
//    @Override
//    public List<SkillsAndInterests> getAllSkillsAndInterests() {
//        return skillsAndInterestsRepository.findAll();
//    }
//
//    @Override
//    public SkillsAndInterests updateSkillsAndInterests(Long id, SkillsAndInterests skillsAndInterests) {
//        if (skillsAndInterestsRepository.existsById(id)) {
//            skillsAndInterests.setId(id);
//            return skillsAndInterestsRepository.save(skillsAndInterests);
//        } else {
//            throw new RuntimeException("Skills and Interests not found");
//        }
//    }
//
//    @Override
//    public void deleteSkillsAndInterests(Long id) {
//        skillsAndInterestsRepository.deleteById(id);
//    }
//}
