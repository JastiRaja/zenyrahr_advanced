package com.zenyrahr.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.LeaveRequest;
import com.zenyrahr.hrms.model.ApprovalModule;
import com.zenyrahr.hrms.model.LeaveStatus;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.LeaveRequestService;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;
    private final S3Service s3Service;
    private final TenantAccessService tenantAccessService;
    private final ApprovalHierarchyService approvalHierarchyService;


    @GetMapping()
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<LeaveRequest> leaveRequests = leaveRequestService.getAllLeaveRequests();
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            leaveRequests = leaveRequests.stream()
                    .filter(req -> req.getEmployee() != null
                            && req.getEmployee().getOrganization() != null
                            && actorOrgId.equals(req.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        leaveRequests.forEach(lr -> {
            List<String> presignedUrls = lr.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList());
            lr.setDocumentUrls(presignedUrls);
        });
        return ResponseEntity.ok(leaveRequests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequestById(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return leaveRequestService.getLeaveRequestById(id)
                .map(leaveRequest -> {
                    tenantAccessService.assertCanAccessEmployee(actor, leaveRequest.getEmployee());
                    List<String> presignedUrls = leaveRequest.getDocumentUrls().stream()
                            .map(this::convertToPresignedUrl)
                            .collect(Collectors.toList());
                    leaveRequest.setDocumentUrls(presignedUrls);
                    return ResponseEntity.ok(leaveRequest);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    private String convertToPresignedUrl(String s3Url) {
        String key;
        if (s3Url.startsWith("http")) {
            // It's a full URL, extract the key
            try {
                URI uri = new URI(s3Url);
                key = uri.getPath().substring(1); // Remove leading slash
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid S3 URL format", e);
            }
        } else {
            // It's already a key
            key = s3Url;
        }
        return s3Service.getPresignedUrl(key);
    }
//    @PostMapping
//    public ResponseEntity<LeaveRequest> saveLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
//        return ResponseEntity.ok(leaveRequestService.saveLeaveRequest(leaveRequest));
//    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveRequest> saveLeaveRequest(
            @RequestPart("leaveRequest") String leaveRequestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        // Parse the JSON string into a LeaveRequest object
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        LeaveRequest leaveRequest = mapper.readValue(leaveRequestJson, LeaveRequest.class);
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, leaveRequest.getEmployee().getId());

        // Save the LeaveRequest to generate its ID and associate an Employee
        LeaveRequest savedTicket = leaveRequestService.saveLeaveRequest(leaveRequest);

        // Only upload documents if files are provided
        if (files != null && !files.isEmpty()) {
            String userId = savedTicket.getEmployee().getId().toString();
            String documentUrl = s3Service.uploadCategoryDocuments(files, userId, "leaveRequest");
            savedTicket.getDocumentUrls().add(documentUrl);
            savedTicket = leaveRequestService.updateLeaveRequest(savedTicket.getId(), savedTicket);
        }

        return ResponseEntity.ok(savedTicket);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByEmployee(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        List<LeaveRequest> leaveRequests = leaveRequestService.getLeaveRequestsByEmployee(employeeId);
        leaveRequests.forEach(lr -> {
            List<String> presignedUrls = lr.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList());
            lr.setDocumentUrls(presignedUrls);
        });
        return ResponseEntity.ok(leaveRequests);
    }

    @GetMapping("/employee/{employeeId}/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaveRequestsByEmployee(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        List<LeaveRequest> leaveRequests = leaveRequestService.getPendingLeaveRequestsByEmployee(employeeId);
        leaveRequests.forEach(lr -> {
            List<String> presignedUrls = lr.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList());
            lr.setDocumentUrls(presignedUrls);
        });
        return ResponseEntity.ok(leaveRequests);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLeaveRequest(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest current = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave Request not found"));
            if (current.getStatus() != LeaveStatus.PENDING) {
                throw new RuntimeException("Only pending leave requests can be approved.");
            }
            approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.LEAVE, 1);
            LeaveRequest leaveRequest = leaveRequestService.approveLeaveRequest(id);
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveRequest> rejectLeaveRequest(@PathVariable Long id){
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest current = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave Request not found"));
            if (current.getStatus() != LeaveStatus.PENDING) {
                throw new RuntimeException("Only pending leave requests can be rejected.");
            }
            approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.LEAVE, 1);
            LeaveRequest leaveRequest = leaveRequestService.rejectLeaveRequest(id);
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<LeaveRequest> withdrawrejectLeaveRequest(@PathVariable Long id){
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest leaveRequest = leaveRequestService.withdrawrejectLeaveRequest(id);
            tenantAccessService.assertCanAccessEmployee(actor, leaveRequest.getEmployee());
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/{id}/revoke-request")
    public ResponseEntity<?> requestRevokeLeaveRequest(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest current = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave Request not found"));
            tenantAccessService.assertCanAccessEmployee(actor, current.getEmployee());
            if (!actor.getId().equals(current.getEmployee().getId())) {
                throw new RuntimeException("Only the employee can request leave revocation.");
            }
            LeaveRequest leaveRequest = leaveRequestService.requestRevokeLeaveRequest(id);
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/approve-revoke")
    public ResponseEntity<?> approveRevokeLeaveRequest(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest current = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave Request not found"));
            if (current.getStatus() != LeaveStatus.APPROVED || !Boolean.TRUE.equals(current.getRevocationRequested())) {
                throw new RuntimeException("Only revocation pending requests can be approved.");
            }
            approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.LEAVE, 1);
            LeaveRequest leaveRequest = leaveRequestService.approveRevokeLeaveRequest(id);
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/reject-revoke")
    public ResponseEntity<?> rejectRevokeLeaveRequest(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            LeaveRequest current = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave Request not found"));
            if (current.getStatus() != LeaveStatus.APPROVED || !Boolean.TRUE.equals(current.getRevocationRequested())) {
                throw new RuntimeException("Only revocation pending requests can be rejected.");
            }
            approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.LEAVE, 1);
            LeaveRequest leaveRequest = leaveRequestService.rejectRevokeLeaveRequest(id);
            return ResponseEntity.ok(leaveRequest);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

}
