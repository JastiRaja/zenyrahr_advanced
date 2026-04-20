package com.zenyrahr.hrms.employeeinformation;

import com.zenyrahr.hrms.Repository.*;
import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.Repository.*;
import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.service.SequenceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class FormDataService {

    private final UserRepository userRepository;
    private final IdentificationDetailsRepository identificationDetailsRepository;
    private final JobInformationRepository jobInformationRepository;
    private final EducationalBackgroundRepository educationalBackgroundRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final SalaryAndBankDetailsRepository salaryAndBankDetailsRepository;
    private final FamilyDetailsRepository familyDetailsRepository;
    private final HealthAndMedicalInformationRepository healthAndMedicalInformationRepository;
//    private final SkillsAndInterestsRepository skillsAndInterestsRepository;
    private final AdditionalInformationRepository additionalInformationRepository;
    private final SequenceService sequenceService;

    // Save new form data
    @Transactional
    public void processFormData(FormDataDTO formDataDTO) {
        if (formDataDTO.getEmployee() != null && formDataDTO.getEmployee().getId() != null) {
            Employee employee = formDataDTO.getEmployee();
            userRepository.save(employee);

            // Proceed with saving the related entities if userId is present
            saveOrUpdateIdentificationDetails(formDataDTO.getIdentificationDetails());
            saveOrUpdateJobInformation(formDataDTO.getJobInformation());
            saveOrUpdateEducationalBackground(formDataDTO.getEducationalBackground());
            saveOrUpdateWorkExperience(formDataDTO.getWorkExperience());
            saveOrUpdateSalaryAndBankDetails(formDataDTO.getSalaryAndBankDetails());
            saveOrUpdateFamilyDetails(formDataDTO.getFamilyDetails());
            saveOrUpdateHealthAndMedicalInformation(formDataDTO.getHealthAndMedicalInformation());
//            saveOrUpdateSkillsAndInterests(formDataDTO.getSkillsAndInterests());
            saveOrUpdateAdditionalInformation(formDataDTO.getAdditionalInformation());
        } else {
            // If userId is not present, throw an exception or return an error response
            throw new IllegalArgumentException("User ID is required.");
        }
    }


    // Update existing form data based on employeeId
    @Transactional
    public void updateFormData(Long employeeId, FormDataDTO formDataDTO) {
        // Check if the userId is present
        if (employeeId != null) {
            // Fetch the existing user by userId
            Employee existingEmployee = userRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));


        } else {
            // If userId is missing, throw an exception or handle error
            throw new IllegalArgumentException("User ID is required to update form data.");
        }
    }


//    AdditionalInformation existingAdditionalInformation = additionalInformationRepository.findById(userid).orElseThrow(() -> new IllegalArgumentException("User not found"));


    private void updateAdditionalInformation(User existingUser, AdditionalInformation additionalInformation) {
        if (additionalInformation != null) {
            Optional<AdditionalInformation> existingAdditionalInformation = additionalInformationRepository.findByEmployee_Id(existingUser.getId());

            if(existingAdditionalInformation.isPresent()){
                AdditionalInformation existingAdditionalInformationInfo = existingAdditionalInformation.get();
                additionalInformation.setWorkPreferences(additionalInformation.getWorkPreferences());
                additionalInformation.setLanguagesKnown(additionalInformation.getLanguagesKnown());
                additionalInformation.setLinkedinProfile(additionalInformation.getLinkedinProfile());
                additionalInformationRepository.save(existingAdditionalInformationInfo);
            }else{
                throw new RuntimeException("additionalInformation is not found ");
            }
        }

    }

    private void updateHealthAndMedicalInformation(User existingUser, HealthAndMedicalInformation healthAndMedicalInformation) {
        if (healthAndMedicalInformation != null) {
            Optional<HealthAndMedicalInformation> existingHealthAndMedicalInformation = healthAndMedicalInformationRepository.findById(existingUser.getId());

            if (existingHealthAndMedicalInformation.isPresent()) {
                HealthAndMedicalInformation existingHealthAndMedicalInformationInfo = existingHealthAndMedicalInformation.get();
                healthAndMedicalInformation.setBloodGroup(healthAndMedicalInformation.getBloodGroup());
                healthAndMedicalInformation.setAllergies(healthAndMedicalInformation.getAllergies());
                healthAndMedicalInformation.setProviderName(healthAndMedicalInformation.getProviderName());
                healthAndMedicalInformation.setPreExistingConditions(healthAndMedicalInformation.getPreExistingConditions());
                healthAndMedicalInformation.setPolicyNumber(healthAndMedicalInformation.getPolicyNumber());
                healthAndMedicalInformation.setValidity(healthAndMedicalInformation.getValidity());
                healthAndMedicalInformationRepository.save(existingHealthAndMedicalInformationInfo);
            } else {
                throw new RuntimeException("healthAndMedicalInformation is not found");
            }
        }
    }

    private void updateSalaryAndBankDetails(User existingUser, SalaryAndBankDetails salaryAndBankDetails) {
        if (salaryAndBankDetails != null) {
            Optional<SalaryAndBankDetails> existingSalaryAndBankDetails = salaryAndBankDetailsRepository.findByEmployee_Id(existingUser.getId());

            if (existingSalaryAndBankDetails.isPresent()) {
                SalaryAndBankDetails existingSalaryAndBankDetailsInfo = existingSalaryAndBankDetails.get();
                salaryAndBankDetails.setCtc(salaryAndBankDetails.getCtc());
                salaryAndBankDetails.setBasic(salaryAndBankDetails.getBasic());
                salaryAndBankDetails.setAllowances(salaryAndBankDetails.getAllowances());
                salaryAndBankDetails.setBankName(salaryAndBankDetails.getBankName());
                salaryAndBankDetails.setAccountNumber(salaryAndBankDetails.getAccountNumber());
                salaryAndBankDetails.setIfscCode(salaryAndBankDetails.getIfscCode());
                salaryAndBankDetails.setAccountHolderName(salaryAndBankDetails.getAccountHolderName());
                salaryAndBankDetailsRepository.save(existingSalaryAndBankDetailsInfo);
            } else {
                throw new RuntimeException("salaryAndBankDetails is not found ");
            }
        }
    }

    private void updateWorkExperience(User existingUser, WorkExperience workExperience) {
        if (workExperience != null) {
            Optional<WorkExperience> existingWorkExperience = workExperienceRepository.findByEmployee_Id(existingUser.getId());

            if (existingWorkExperience.isPresent()) {
                WorkExperience existingWorkExperienceInfo = existingWorkExperience.get();
                workExperience.setTotalWorkExperience(workExperience.getTotalWorkExperience());
                workExperience.setCompanyName(workExperience.getCompanyName());
                workExperience.setDesignation(workExperience.getDesignation());
                workExperience.setStartDate(workExperience.getStartDate());
                workExperience.setEndDate(workExperience.getEndDate());
                workExperience.setRolesAndResponsibilities(workExperience.getRolesAndResponsibilities());
                workExperienceRepository.save(existingWorkExperienceInfo);
            }

        }
    }

    private void updateEducationalBackground(User existingUser, EducationalBackground educationalBackground) {

        if (educationalBackground != null) {
            Optional<EducationalBackground> existingEducationalBackground = educationalBackgroundRepository.findById(existingUser.getId());

            if (existingEducationalBackground.isPresent()) {
                EducationalBackground existingEducationalBackgroundInfo = existingEducationalBackground.get();
                educationalBackground.setHighestQualification(educationalBackground.getHighestQualification());
                educationalBackground.setDegreeName(educationalBackground.getDegreeName());
                educationalBackground.setFieldOfStudy(educationalBackground.getFieldOfStudy());
                educationalBackground.setUniversityName(educationalBackground.getUniversityName());
                educationalBackground.setYearOfPassing(educationalBackground.getYearOfPassing());
                educationalBackground.setCertifications(educationalBackground.getCertifications());
                educationalBackgroundRepository.save(existingEducationalBackgroundInfo);
            } else {
                throw new RuntimeException("educationalBackground not found");
            }
        }
    }

    private void updateJobInformation(User existingUser, JobInformation jobInformation) {
        if (jobInformation != null) {
            Optional<JobInformation> existingJobInformation = jobInformationRepository.findById(existingUser.getId());

            if(existingJobInformation.isPresent()){
                JobInformation existingJobInformationInfo = existingJobInformation.get();
                jobInformation.setDesignation(jobInformation.getDesignation());
                jobInformation.setDepartment(jobInformation.getDepartment());
                jobInformation.setJobType(jobInformation.getJobType());
                jobInformation.setEmploymentStatus(jobInformation.getEmploymentStatus());
                jobInformation.setDateOfJoining(jobInformation.getDateOfJoining());
                jobInformation.setReportingManager(jobInformation.getReportingManager());
                jobInformation.setWorkLocation(jobInformation.getWorkLocation());
                jobInformation.setEmployeeGrade(jobInformation.getEmployeeGrade());
                jobInformationRepository.save(existingJobInformationInfo);
            } else {
                throw new EntityNotFoundException("jobInformation not found for userId: " + existingUser.getId());
            }
        } else {
            throw new RuntimeException("jobInformation not found");
        }
    }

    private void updateIdentificationDetails(User existingUser, IdentificationDetails identificationDetails) {

//        if{
            Optional<IdentificationDetails> existingIdentificationDetails = identificationDetailsRepository.findById(existingUser.getId());

            if(existingIdentificationDetails.isPresent()){
                IdentificationDetails existingIdentificationDetailsInfo = existingIdentificationDetails.get();
                identificationDetails.setAadharNumber(identificationDetails.getAadharNumber());
                identificationDetails.setPanCard(identificationDetails.getPanCard());
                identificationDetails.setPassportNumber(identificationDetails.getPassportNumber());
                identificationDetails.setDrivingLicense(identificationDetails.getDrivingLicense());
                identificationDetails.setIdProofDocuments(identificationDetails.getIdProofDocuments());
                identificationDetailsRepository.save(existingIdentificationDetailsInfo);
            }else {
                throw new EntityNotFoundException("IdentificationDetails not found for userId: " + existingUser.getId());
            }
//                }
    }


    private void saveOrUpdateIdentificationDetails(IdentificationDetails identificationDetails) {
//
        if (identificationDetails != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("IDENTITY");
            identificationDetails.setCode(generatedCode); // Ensure code is set
            identificationDetailsRepository.save(identificationDetails);
        } else {
            throw new IllegalArgumentException("identificationDetails must not be null.");
        }
    }

    private void saveOrUpdateJobInformation(JobInformation jobInformation) {

        if (jobInformation != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("JOBINFO");
            jobInformation.setCode(generatedCode); // Ensure code is set
            jobInformationRepository.save(jobInformation);
        } else {
            throw new IllegalArgumentException("jobInformation must not be null.");
        }
    }

    private void saveOrUpdateEducationalBackground(EducationalBackground educationalBackground) {
        if (educationalBackground != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("EDUCATION-BACKGROUND");
            educationalBackground.setCode(generatedCode); // Ensure code is set
            educationalBackgroundRepository.save(educationalBackground);
        } else {
            throw new IllegalArgumentException("educationalBackground must not be null.");
        }
    }

    private void saveOrUpdateWorkExperience(WorkExperience workExperience) {

        if (workExperience != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("WORKEXP");
            workExperience.setCode(generatedCode); // Ensure code is set
            workExperienceRepository.save(workExperience);
        } else {
            throw new IllegalArgumentException("workExperience must not be null.");
        }
    }

    private void saveOrUpdateSalaryAndBankDetails(SalaryAndBankDetails salaryAndBankDetails) {

        if (salaryAndBankDetails != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("SAL&BANKDET");
            salaryAndBankDetails.setCode(generatedCode); // Ensure code is set
            salaryAndBankDetailsRepository.save(salaryAndBankDetails);
        } else {
            throw new IllegalArgumentException("salaryAndBankDetails must not be null.");
        }
    }

    private void saveOrUpdateFamilyDetails(FamilyDetails familyDetails) {

        if (familyDetails != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("FAMILYDET");
//            familyDetails.setCode(generatedCode); // Ensure code is set
            familyDetailsRepository.save(familyDetails);
        } else {
            throw new IllegalArgumentException("familyDetails must not be null.");
        }
    }

    private void saveOrUpdateHealthAndMedicalInformation(HealthAndMedicalInformation healthAndMedicalInformation) {

        if (healthAndMedicalInformation != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("H&MINFO");
            healthAndMedicalInformation.setCode(generatedCode); // Ensure code is set
            healthAndMedicalInformationRepository.save(healthAndMedicalInformation);
        } else {
            throw new IllegalArgumentException("healthAndMedicalInformation must not be null.");
        }
    }

    private void saveOrUpdateAdditionalInformation(AdditionalInformation additionalInformation) {

        if (additionalInformation != null) {
            // Generate a unique code for the PersonalInformation entity
            String generatedCode = sequenceService.getNextCode("ADDINFO");
            additionalInformation.setCode(generatedCode); // Ensure code is set
            additionalInformationRepository.save(additionalInformation);
        } else {
            throw new IllegalArgumentException("additionalInformation must not be null.");
        }
    }

    private final List<FormDataDTO> formDataStore = new ArrayList<>(); // In-memory data store

    public FormDataDTO getFormDataById(Long id) {
        return formDataStore.stream()
                .filter(data -> data.getEmployee().getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Form data not found for ID: " + id));
    }

    public List<FormDataDTO> getAllFormData() {
        return new ArrayList<>(formDataStore);
    }
}


