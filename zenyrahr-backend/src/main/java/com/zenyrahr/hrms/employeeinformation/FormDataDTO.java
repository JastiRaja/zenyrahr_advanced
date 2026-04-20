package com.zenyrahr.hrms.employeeinformation;

import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.model.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FormDataDTO {
    private Employee employee;
    private MultipartFile profileImage;
//    private PersonalInformation personalInformation;
    private IdentificationDetails identificationDetails;
    private JobInformation jobInformation;
    private EducationalBackground educationalBackground;
    private WorkExperience workExperience;
    private SalaryAndBankDetails salaryAndBankDetails;
    private FamilyDetails familyDetails;
    private HealthAndMedicalInformation healthAndMedicalInformation;
//    private SkillsAndInterests skillsAndInterests;
    private AdditionalInformation additionalInformation;
}
