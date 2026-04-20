//package com.talvox.hrms.employeeinformation;
//
//import com.talvox.hrms.employeeinformation.FormDataDTO;
//import com.talvox.hrms.employeeinformation.FormDataService;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class FormDataServiceImpl implements FormDataService {
//
//    private final List<FormDataDTO> database = new ArrayList<>(); // In-memory list for demonstration
//
//    @Override
//    public void processFormData(FormDataDTO formDataDTO) {
//        database.add(formDataDTO); // Save new data
//    }
//
//    @Override
//    public List<FormDataDTO> getAllFormData() {
//        return database; // Return all data
//    }
//
//    @Override
//    public FormDataDTO getFormDataById(Long id) {
//        return database.stream()
//                .filter(data -> data.getId().equals(id))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Form data not found."));
//    }
//
//    @Override
//    public void updateFormData(Long id, FormDataDTO formDataDTO) {
//        FormDataDTO existing = getFormDataById(id);
//        existing.setName(formDataDTO.getName()); // Example field
//        existing.setValue(formDataDTO.getValue()); // Example field
//    }
//
//    @Override
//    public void deleteFormData(Long id) {
//        FormDataDTO existing = getFormDataById(id);
//        database.remove(existing);
//    }
//}
