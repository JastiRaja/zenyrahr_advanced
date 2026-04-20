package com.zenyrahr.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zenyrahr.hrms.dto.TravelRequestResponseDTO;
import com.zenyrahr.hrms.model.ApprovalModule;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.TravelRequest;
import com.zenyrahr.hrms.model.TravelStatus;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.TravelRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/travel-requests")
public class TravelRequestController {

    @Autowired
    private TravelRequestService travelRequestService;

    @Autowired
    private S3Service s3Service;
    @Autowired
    private TenantAccessService tenantAccessService;
    @Autowired
    private ApprovalHierarchyService approvalHierarchyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TravelRequestResponseDTO> createTravelRequest(
            @RequestPart("travelRequest") String travelRequestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        // Parse JSON to TravelRequest object
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        TravelRequest travelRequest = mapper.readValue(travelRequestJson, TravelRequest.class);
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, travelRequest.getEmployee().getId());

        // Save the TravelRequest to generate ID
        TravelRequest savedRequest = travelRequestService.saveTravelRequest(travelRequest);

        // Upload documents only when files are provided.
        TravelRequest updatedRequest = savedRequest;
        if (files != null && !files.isEmpty()) {
            String userId = savedRequest.getEmployee().getId().toString(); // Ensure Employee is associated
            String documentUrl = s3Service.uploadCategoryDocuments(files, userId, "travel_requests");
            savedRequest.getDocumentUrls().add(documentUrl);
            updatedRequest = travelRequestService.updateTravelRequest(savedRequest.getId(), savedRequest);
        }

        return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(updatedRequest));
    }

    // Get all Travel Requests
    @GetMapping
    public ResponseEntity<List<TravelRequestResponseDTO>> getAllTravelRequests() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<TravelRequest> travelRequests = travelRequestService.getAllTravelRequests();
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            travelRequests = travelRequests.stream()
                    .filter(req -> req.getEmployee() != null
                            && req.getEmployee().getOrganization() != null
                            && actorOrgId.equals(req.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        
        // Convert document URLs to presigned URLs for each travel request
        travelRequests.forEach(request -> {
            if (request.getDocumentUrls() != null && !request.getDocumentUrls().isEmpty()) {
                List<String> presignedUrls = request.getDocumentUrls().stream()
                    .map(this::convertToPresignedUrl)
                    .collect(Collectors.toList());
                request.setDocumentUrls(presignedUrls);
            }
        });
        
        List<TravelRequestResponseDTO> responseDTOs = travelRequests.stream()
                .map(TravelRequestResponseDTO::fromTravelRequest)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(responseDTOs);
    }

    // Get a single Travel Request by ID
    @GetMapping("/{id}")
    public ResponseEntity<TravelRequestResponseDTO> getTravelRequestById(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return travelRequestService.getTravelRequestById(id)
                .map(travelRequest -> {
                    tenantAccessService.assertCanAccessEmployee(actor, travelRequest.getEmployee());
                    if (travelRequest.getDocumentUrls() != null && !travelRequest.getDocumentUrls().isEmpty()) {
                        List<String> presignedUrls = travelRequest.getDocumentUrls().stream()
                                .map(this::convertToPresignedUrl)
                                .collect(Collectors.toList());
                        travelRequest.setDocumentUrls(presignedUrls);
                    }
                    return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(travelRequest));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TravelRequestResponseDTO>> getTravelRequestsByEmployeeId(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        List<TravelRequest> travelRequests = travelRequestService.getTravelRequestByEmployeeId(employeeId);
        
        // Convert document URLs to presigned URLs for each travel request
        travelRequests.forEach(request -> {
            if (request.getDocumentUrls() != null && !request.getDocumentUrls().isEmpty()) {
                List<String> presignedUrls = request.getDocumentUrls().stream()
                    .map(this::convertToPresignedUrl)
                    .collect(Collectors.toList());
                request.setDocumentUrls(presignedUrls);
            }
        });
        
        List<TravelRequestResponseDTO> responseDTOs = travelRequests.stream()
                .map(TravelRequestResponseDTO::fromTravelRequest)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(responseDTOs);
    }

    private String convertToPresignedUrl(String s3Url) {
        try {
            // If the URL is already a full S3 URL, extract just the key part
            if (s3Url.startsWith("http")) {
                URI uri = new URI(s3Url);
                String path = uri.getPath();
                // Remove the first slash if present
                return s3Service.getPresignedUrl(path.startsWith("/") ? path.substring(1) : path);
            }
            // If it's just the key, use it directly
            return s3Service.getPresignedUrl(s3Url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid S3 URL format: " + s3Url, e);
        }
    }

    // Update a Travel Request
    @PutMapping("/{id}")
    public ResponseEntity<TravelRequestResponseDTO> updateTravelRequest(@PathVariable Long id, @RequestBody TravelRequest travelRequestDetails) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, travelRequestDetails.getEmployee().getId());
        TravelRequest updatedRequest = travelRequestService.updateTravelRequest(id, travelRequestDetails);
        return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(updatedRequest));
    }

    // Delete a Travel Request
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTravelRequest(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            TravelRequest existing = travelRequestService.getTravelRequestById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Travel request not found"));
            tenantAccessService.assertCanAccessEmployee(actor, existing.getEmployee());
            travelRequestService.deleteTravelRequest(id);
            return ResponseEntity.ok(Map.of("message", "Travel Request Deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve-first-level")
    public ResponseEntity<TravelRequestResponseDTO> approveFirstLevel(
        @PathVariable Long id,
        @RequestParam(name="approverId", required=false) Long approverId,
        @RequestParam(name="approverid", required=false) Long approverIdAlt,
        @RequestParam(name="comments") String approvalComments1 )
     {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            Long finalApproverId = approverId != null ? approverId : approverIdAlt;
            if (finalApproverId != null && !tenantAccessService.isMainAdmin(actor) && !finalApproverId.equals(actor.getId())) {
                throw new IllegalArgumentException("Approver ID must match authenticated user.");
            }
            finalApproverId = actor.getId();
            TravelRequest current = travelRequestService.getTravelRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Travel Request not found"));
            approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.TRAVEL, 1);
            TravelRequest travelRequest = travelRequestService.approveFirstLevel(id, finalApproverId, approvalComments1);
            return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(travelRequest));
     }

    @PutMapping("/{id}/approve-second-level")
    public ResponseEntity<TravelRequestResponseDTO> approveSecondLevel(
            @PathVariable Long id,
            @RequestParam(name="approverId", required=false) Long approverId,
            @RequestParam(name="approverid", required=false) Long approverIdAlt,
            @RequestParam(name="comments") String approvalComments2 )
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long finalApproverId = approverId != null ? approverId : approverIdAlt;
        if (finalApproverId != null && !tenantAccessService.isMainAdmin(actor) && !finalApproverId.equals(actor.getId())) {
            throw new IllegalArgumentException("Approver ID must match authenticated user.");
        }
        finalApproverId = actor.getId();
        TravelRequest current = travelRequestService.getTravelRequestById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.TRAVEL, 2);
        TravelRequest travelRequest = travelRequestService.approveSecondLevel(id, finalApproverId, approvalComments2);
        return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(travelRequest));
    }

    @PutMapping("/{id}/reject-first-level")
    public ResponseEntity<TravelRequestResponseDTO> rejectFirstLevel(
            @PathVariable Long id,
            @RequestParam(name="approverId", required=false) Long approverId,
            @RequestParam(name="approverid", required=false) Long approverIdAlt,
            @RequestParam(name="comments") String rejectionComments )
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long finalApproverId = approverId != null ? approverId : approverIdAlt;
        if (finalApproverId != null && !tenantAccessService.isMainAdmin(actor) && !finalApproverId.equals(actor.getId())) {
            throw new IllegalArgumentException("Approver ID must match authenticated user.");
        }
        finalApproverId = actor.getId();
        TravelRequest current = travelRequestService.getTravelRequestById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.TRAVEL, 1);
        TravelRequest rejectedTravelRequest = travelRequestService.rejectFirstLevel(id, finalApproverId, rejectionComments);
        return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(rejectedTravelRequest));
    }

    @PutMapping("/{id}/reject-second-level")
    public ResponseEntity<TravelRequestResponseDTO> rejectSecondLevel(
            @PathVariable Long id,
            @RequestParam(name="approverId", required=false) Long approverId,
            @RequestParam(name="approverid", required=false) Long approverIdAlt,
            @RequestParam(name="comments") String rejectionComments )
    {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        Long finalApproverId = approverId != null ? approverId : approverIdAlt;
        if (finalApproverId != null && !tenantAccessService.isMainAdmin(actor) && !finalApproverId.equals(actor.getId())) {
            throw new IllegalArgumentException("Approver ID must match authenticated user.");
        }
        finalApproverId = actor.getId();
        TravelRequest current = travelRequestService.getTravelRequestById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.TRAVEL, 2);
        TravelRequest rejectedTravelRequest = travelRequestService.rejectSecondLevel(id, finalApproverId, rejectionComments);
        return ResponseEntity.ok(TravelRequestResponseDTO.fromTravelRequest(rejectedTravelRequest));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TravelRequestResponseDTO>> getPendingTravelRequests(@RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (employeeId != null) {
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        }
        List<TravelRequest> pendingFirstLevel = (employeeId == null)
            ? travelRequestService.getTravelRequestsByFirstLevelApprovalStatus(TravelStatus.PENDING)
            : travelRequestService.getTravelRequestsByFirstLevelApprovalStatusAndEmployeeId(TravelStatus.PENDING, employeeId);
        List<TravelRequest> pendingSecondLevel = (employeeId == null)
            ? travelRequestService.getPendingSecondLevelTravelRequests()
            : travelRequestService.getPendingSecondLevelTravelRequestsByEmployeeId(employeeId);
        // Combine and remove duplicates
        List<TravelRequest> allPending = new java.util.ArrayList<>(pendingFirstLevel);
        for (TravelRequest t : pendingSecondLevel) {
            if (!allPending.contains(t)) allPending.add(t);
        }
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            allPending = allPending.stream()
                    .filter(req -> req.getEmployee() != null
                            && req.getEmployee().getOrganization() != null
                            && actorOrgId.equals(req.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        allPending = allPending.stream()
                .filter(req -> isActionableForActor(actor, req))
                .collect(Collectors.toList());
        allPending.forEach(request -> {
            if (request.getDocumentUrls() != null && !request.getDocumentUrls().isEmpty()) {
                List<String> presignedUrls = request.getDocumentUrls().stream()
                    .map(this::convertToPresignedUrl)
                    .collect(Collectors.toList());
                request.setDocumentUrls(presignedUrls);
            }
        });
        List<TravelRequestResponseDTO> responseDTOs = allPending.stream()
                .map(TravelRequestResponseDTO::fromTravelRequest)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    private boolean isActionableForActor(Employee actor, TravelRequest request) {
        if (request == null || request.getEmployee() == null) {
            return false;
        }
        boolean isFirstLevelPending = request.getFirstLevelApprovalStatus() == TravelStatus.PENDING;
        boolean isSecondLevelPending = request.getFirstLevelApprovalStatus() == TravelStatus.APPROVED
                && request.getSecondLevelApprovalStatus() == TravelStatus.PENDING;
        int levelNo = isFirstLevelPending ? 1 : (isSecondLevelPending ? 2 : -1);
        if (levelNo < 0) {
            return false;
        }
        try {
            approvalHierarchyService.assertActorCanApprove(actor, request.getEmployee(), ApprovalModule.TRAVEL, levelNo);
            return true;
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    @GetMapping("/approved")
    public ResponseEntity<List<TravelRequestResponseDTO>> getApprovedTravelRequests(@RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (employeeId != null) {
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        }
        List<TravelRequest> travelRequests = (employeeId == null)
            ? travelRequestService.getFullyApprovedTravelRequests()
            : travelRequestService.getFullyApprovedTravelRequestsByEmployeeId(employeeId);
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            travelRequests = travelRequests.stream()
                    .filter(req -> req.getEmployee() != null
                            && req.getEmployee().getOrganization() != null
                            && actorOrgId.equals(req.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        travelRequests.forEach(request -> {
            if (request.getDocumentUrls() != null && !request.getDocumentUrls().isEmpty()) {
                List<String> presignedUrls = request.getDocumentUrls().stream()
                    .map(this::convertToPresignedUrl)
                    .collect(Collectors.toList());
                request.setDocumentUrls(presignedUrls);
            }
        });
        List<TravelRequestResponseDTO> responseDTOs = travelRequests.stream()
                .map(TravelRequestResponseDTO::fromTravelRequest)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }
}
