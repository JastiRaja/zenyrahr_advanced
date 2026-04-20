package com.zenyrahr.hrms.employeeinformation;

import com.zenyrahr.hrms.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/form-data")
@RequiredArgsConstructor
public class FormDataController {

    private final FormDataService formDataService;
    private final S3Service s3Service;

    @GetMapping("/{id}")
    public ResponseEntity<FormDataDTO> getFormData(@PathVariable Long id) {
        FormDataDTO formDataDTO = formDataService.getFormDataById(id);
        return ResponseEntity.ok(formDataDTO);
    }

    @GetMapping
    public ResponseEntity<List<FormDataDTO>> getAllFormData() {
        List<FormDataDTO> formDataList = formDataService.getAllFormData();
        return ResponseEntity.ok(formDataList);
    }


    @PostMapping
    public ResponseEntity<String> processFormData(@RequestBody FormDataDTO formDataDTO) {
        formDataService.processFormData(formDataDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("Form data saved successfully.");
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> processFormData(
            @RequestPart("data") FormDataDTO formDataDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = s3Service.uploadProfilePicture(
                        imageFile,
                        formDataDTO.getEmployee().getId().toString()
                );
//                formDataDTO.getEmployee().setProfileImageUrl(imagePath);
            }

            formDataService.processFormData(formDataDTO);
            return ResponseEntity.ok("Form data processed successfully");

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error processing form: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateFormData(@PathVariable Long id, @RequestBody FormDataDTO formDataDTO) {
        Long User;
        formDataService.updateFormData(id, formDataDTO);
        return ResponseEntity.ok("Form data updated successfully.");
    }


}
