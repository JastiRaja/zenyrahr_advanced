package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Documents;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.DocumentsService;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.UserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;
    private final DocumentsService documentsService;
    private final UserDetailsService userDetailsService ;
    private final TenantAccessService tenantAccessService;
    private static final List<String> VALID_CATEGORIES = Arrays.asList(
            "education", "medical", "experience", "personal","service_ticket","leaveRequest","travelrequests", "expenses"
    );

    public S3Controller(S3Service s3Service,
                        DocumentsService documentsService,
                        UserDetailsService userDetailsService,
                        TenantAccessService tenantAccessService) {
        this.s3Service = s3Service;
        this.documentsService = documentsService;
        this.userDetailsService= userDetailsService ;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping("/profile-picture/{employeeId}")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long employeeId) {
        try {
            assertEmployeeAccess(employeeId);
            validateFile(file);
            String s3key = s3Service.uploadProfilePicture(file, employeeId.toString());
            updateEmployeeDocuments(employeeId, "profileImageUrl", s3key);
            return ResponseEntity.ok().body(
                    new UploadResponse("Profile picture uploaded successfully", s3key)
            );
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(e.getMessage())
            );
        }
    }

    @GetMapping("/profile-picture/{employeeId}")
    public ResponseEntity<?> getProfilePicture(@PathVariable Long employeeId) {
        try {
            assertEmployeeAccess(employeeId);
            // Fetch employee details from your database
            Employee employee = userDetailsService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

            Documents documents = employee.getDocuments();
            if (documents == null || documents.getProfileImageUrl() == null) {
                return ResponseEntity.badRequest().body("Profile picture not found");
            }

            // Fetch profile picture URL (S3 key)
//            String s3key = documents.getProfileImageUrl();
            // Generate a pre-signed URL using the S3Service
            String presignedUrl = s3Service.getPresignedUrl(documents.getProfileImageUrl());

            // Return the pre-signed URL to the client
            return ResponseEntity.ok().body(new FileUrlResponse(presignedUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/documents/{employeeId}/{category}")
    public ResponseEntity<?> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @PathVariable Long employeeId,
            @PathVariable String category) {
        try {
            assertEmployeeAccess(employeeId);
            validateCategory(category);
            files.forEach(this::validateFile);

            String fileUrl = s3Service.uploadCategoryDocuments(files, employeeId.toString(), category);
            updateEmployeeDocuments(employeeId, getDocumentFieldName(category), fileUrl);

            return ResponseEntity.ok().body(
                    new UploadResponse(category + " documents uploaded successfully", fileUrl)
            );
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(e.getMessage())
            );
        }
    }

    @GetMapping("/file-url")
    public ResponseEntity<?> getFileUrl(
            @RequestParam String category,
            @RequestParam String fileName) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            if (!tenantAccessService.isMainAdmin(actor)) {
                throw new ResponseStatusException(FORBIDDEN, "Only main admin can access generic file URL endpoint");
            }
            validateCategory(category);
            String url = s3Service.getFileUrl("documents/" + category + "/" + fileName);
            return ResponseEntity.ok().body(new FileUrlResponse(url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/documents/{employeeId}/{category}")
    public ResponseEntity<?> listFiles(
            @PathVariable Long employeeId,
            @PathVariable String category,
            @RequestParam(required = false) String userId) {
//
        try {
            assertEmployeeAccess(employeeId);
            Employee employee = userDetailsService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

            String fieldValue = getDocumentFieldValue(employee.getDocuments(), category);
            if (fieldValue == null) return ResponseEntity.notFound().build();

            String presignedUrl = s3Service.getPresignedUrl(fieldValue);
            return ResponseEntity.ok(new FileUrlResponse(presignedUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/update-file/{employeeId}/{category}")
    public ResponseEntity<?> updateFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long employeeId,
            @PathVariable String category) {
        try {
            assertEmployeeAccess(employeeId);
            validateCategory(category);
            validateFile(file);

            String fileUrl = s3Service.updateCategoryDocument(
                    file,
                    employeeId.toString(),
                    category
            );

            updateEmployeeDocuments(employeeId, getDocumentFieldName(category), fileUrl);

            return ResponseEntity.ok().body(
                    new UploadResponse(category + " document updated successfully", fileUrl)
            );
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{employeeId}/{category}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long employeeId,
            @PathVariable String category) {
        try {
            assertEmployeeAccess(employeeId);
            s3Service.deleteCategoryDocument(employeeId.toString(), category);
            clearDocumentField(employeeId, category);
            return ResponseEntity.ok(new MessageResponse("Documents deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private void updateEmployeeDocuments(Long employeeId, String fieldName, String url) {
        Employee employee = userDetailsService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        Documents document = Optional.ofNullable(employee.getDocuments())
                .orElseGet(() -> {
                    Documents newDocs = new Documents();
                    newDocs.setEmployee(employee);
                    employee.setDocuments(newDocs);
                    return newDocs;
                });

        switch (fieldName) {
            case "profileImageUrl" -> document.setProfileImageUrl(url);
            case "educationDocumentsUrl" -> document.setEducationDocumentsUrl(url);
            case "medicalDocumentsUrl" -> document.setMedicalDocumentsUrl(url);
            case "experienceDocumentsUrl" -> document.setExperienceDocumentsUrl(url);
            case "personalDocumentsUrl" -> document.setPersonalDocumentsUrl(url);
        }

        documentsService.saveDocuments(document);
    }

    private void clearDocumentField(Long employeeId, String category) {
        Employee employee = userDetailsService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        Documents documents = employee.getDocuments();
        if (documents != null) {
            String fieldName = getDocumentFieldName(category);
            switch (fieldName) {
                case "educationDocumentsUrl" -> documents.setEducationDocumentsUrl(null);
                case "medicalDocumentsUrl" -> documents.setMedicalDocumentsUrl(null);
                case "experienceDocumentsUrl" -> documents.setExperienceDocumentsUrl(null);
                case "personalDocumentsUrl" -> documents.setPersonalDocumentsUrl(null);

            }
            documentsService.saveDocuments(documents);
        }

//        private String getDocumentFieldName (String category){
//            return category + "DocumentsUrl";
//        }
    }
    private void validateCategory(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid document category. Valid categories are: "
                    + String.join(", ", VALID_CATEGORIES));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }
    private String getDocumentFieldName(String category) {
        return switch (category.toLowerCase()) {
            case "education" -> "educationDocumentsUrl";
            case "medical" -> "medicalDocumentsUrl";
            case "experience" -> "experienceDocumentsUrl";
            case "personal" -> "personalDocumentsUrl";
            case "travelrequests" ->"documentUrls";
            case "expenses" ->"documentUrls";
            case "service_ticket" ->"documentUrls";
            case "leaveRequest" -> "documentUrls";
            default -> throw new IllegalArgumentException("Invalid category");
        };
    }
    private String getDocumentFieldValue(Documents docs, String category) {
        if (docs == null) return null;
        return switch (category.toLowerCase()) {
            case "education" -> docs.getEducationDocumentsUrl();
            case "medical" -> docs.getMedicalDocumentsUrl();
            case "experience" -> docs.getExperienceDocumentsUrl();
            case "personal" -> docs.getPersonalDocumentsUrl();

            default -> null;
        };
    }
    // Response DTOs
    private record UploadResponse(String message, String fileUrl) {}
    private record FileUrlResponse(String url) {}
    private record FileListResponse(List<String> files) {}
    private record MessageResponse(String message) {}
    private record ErrorResponse(String error) {}

    private void assertEmployeeAccess(Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
    }
}