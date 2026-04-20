package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.ExcelReaderService;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/excel")
public class ExcelUploadController {

    private final ExcelReaderService excelReaderService;
    private final TenantAccessService tenantAccessService;

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            if (!tenantAccessService.canManageEmployees(actor)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            tenantAccessService.assertOrganizationActive(actor);
            // Process the uploaded file (Convert Excel to PDF)
            byte[] pdfContent = excelReaderService.readExcelFile(file);

            // Return the PDF as a downloadable response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "payslip.pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            // Return an error response if something goes wrong
            return ResponseEntity.status(500).body(null);
        }
    }
}
