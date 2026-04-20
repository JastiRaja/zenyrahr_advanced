//package com.talvox.hrms.service.impl;
//
//import com.talvox.hrms.model.PersonalInformation;
//import com.talvox.hrms.Repository.PersonalInformationRepository;
//import com.talvox.hrms.service.PersonalInformationService;
//import com.talvox.hrms.service.SequenceService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class PersonalInformationServiceImpl implements PersonalInformationService {
//
//    private final PersonalInformationRepository personalInformationRepository;
//    private final SequenceService sequenceService;
//
//    @Override
//    public PersonalInformation createPersonalInformation(PersonalInformation personalInformation) {
//        personalInformation.setCode(sequenceService.getNextCode("PERSONINFO"));
//        return personalInformationRepository.save(personalInformation);
//    }
//
//    @Override
//    public Optional<PersonalInformation> getPersonalInformationById(Long id) {
//        return personalInformationRepository.findById(id);
//    }
//
//    @Override
//    public List<PersonalInformation> getAllPersonalInformation() {
//        return personalInformationRepository.findAll();
//    }
//
//    @Override
//    public PersonalInformation updatePersonalInformation(Long id, PersonalInformation personalInformation) {
//        if (personalInformationRepository.existsById(id)) {
//            personalInformation.setId(id);
//            return personalInformationRepository.save(personalInformation);
//        } else {
//            throw new RuntimeException("Personal Information not found");
//        }
//    }
//
//    @Override
//    public void deletePersonalInformation(Long id) {
//        personalInformationRepository.deleteById(id);
//    }
//}
