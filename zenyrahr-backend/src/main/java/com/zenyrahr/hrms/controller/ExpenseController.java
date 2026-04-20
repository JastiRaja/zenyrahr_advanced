package com.zenyrahr.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zenyrahr.hrms.dto.ExpenseResponseDTO;
import com.zenyrahr.hrms.model.ApprovalModule;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Expense;
import com.zenyrahr.hrms.model.ExpenseStatus;
import com.zenyrahr.hrms.service.ApprovalHierarchyService;
import com.zenyrahr.hrms.service.ExpenseService;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.TenantAccessService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private S3Service s3Service;
    @Autowired
    private TenantAccessService tenantAccessService;
    @Autowired
    private ApprovalHierarchyService approvalHierarchyService;

    // Create a new Expense with file uploads
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseResponseDTO> createExpense(
            @RequestPart("expense") String expenseJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Expense expense = mapper.readValue(expenseJson, Expense.class);
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanAccessEmployeeId(actor, expense.getEmployee().getId());

        if (expense.getId() != null) {
            throw new IllegalArgumentException("New expense must not include an ID.");
        }

        // Save to generate ID
        Expense savedExpense = expenseService.saveExpense(expense);

        // Upload documents and collect keys
        if (files != null && !files.isEmpty()) {
            String userId = savedExpense.getEmployee().getId().toString();
            List<String> keys = Collections.singletonList(s3Service.uploadCategoryDocuments(files, userId, "expenses"));
            savedExpense.getDocumentUrls().addAll(keys);
            savedExpense = expenseService.updateExpense(savedExpense.getId(), savedExpense);
        }

        // Convert to presigned URLs
        List<String> presigned = savedExpense.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList());
        savedExpense.setDocumentUrls(presigned);

        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(savedExpense));
    }

    // Get all Expenses
    @GetMapping
    public ResponseEntity<List<ExpenseResponseDTO>> getAllExpenses() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<Expense> expenses = expenseService.getAllExpenses();
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            expenses = expenses.stream()
                    .filter(exp -> exp.getEmployee() != null
                            && exp.getEmployee().getOrganization() != null
                            && actorOrgId.equals(exp.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        List<ExpenseResponseDTO> dtos = expenses.stream()
                .peek(exp -> exp.setDocumentUrls(
                        exp.getDocumentUrls().stream()
                                .map(this::convertToPresignedUrl)
                                .collect(Collectors.toList())
                ))
                .map(ExpenseResponseDTO::fromExpense)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get Expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return expenseService.getExpenseById(id)
                .map(exp -> {
                    tenantAccessService.assertCanAccessEmployee(actor, exp.getEmployee());
                    exp.setDocumentUrls(
                            exp.getDocumentUrls().stream()
                                    .map(this::convertToPresignedUrl)
                                    .collect(Collectors.toList())
                    );
                    return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(exp));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get Expenses by Employee ID
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ExpenseResponseDTO>> getByEmployee(@PathVariable Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        List<Expense> expenses = expenseService.getExpenseByEmployeeId(employeeId);
        List<ExpenseResponseDTO> dtos = expenses.stream()
                .peek(exp -> exp.setDocumentUrls(
                        exp.getDocumentUrls().stream()
                                .map(this::convertToPresignedUrl)
                                .collect(Collectors.toList())
                ))
                .map(ExpenseResponseDTO::fromExpense)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get just the document URLs for an Expense
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<String>> getExpenseDocuments(@PathVariable Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        return expenseService.getExpenseById(id)
                .map(exp -> {
                    tenantAccessService.assertCanAccessEmployee(actor, exp.getEmployee());
                    return exp.getDocumentUrls().stream()
                            .map(this::convertToPresignedUrl)
                            .collect(Collectors.toList());
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update an Expense (without files)
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(
            @PathVariable Long id,
            @RequestBody Expense expenseDetails
    ) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertCanAccessEmployeeId(actor, expenseDetails.getEmployee().getId());
        Expense updated = expenseService.updateExpense(id, expenseDetails);
        updated.setDocumentUrls(
                updated.getDocumentUrls().stream()
                        .map(this::convertToPresignedUrl)
                        .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(updated));
    }

    // Delete an Expense
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long id) {
        try {
            Employee actor = tenantAccessService.requireCurrentEmployee();
            Expense expense = expenseService.getExpenseById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
            tenantAccessService.assertCanAccessEmployee(actor, expense.getEmployee());
            expenseService.deleteExpense(id);
            return ResponseEntity.ok(Map.of("message", "Expense Request Deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve-first-level")
    public ResponseEntity<ExpenseResponseDTO> approveFirstLevel(
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
        Expense current = expenseService.getExpenseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.EXPENSE, 1);
        Expense expense = expenseService.approveFirstLevel(id, finalApproverId, approvalComments1);
        expense.setDocumentUrls(
            expense.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(expense));
    }

    @PutMapping("/{id}/approve-second-level")
    public ResponseEntity<ExpenseResponseDTO> approveSecondLevel(
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
        Expense current = expenseService.getExpenseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.EXPENSE, 2);
        Expense expense = expenseService.approveSecondLevel(id, finalApproverId, approvalComments2);
        expense.setDocumentUrls(
            expense.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(expense));
    }

    @PutMapping("/{id}/reject-first-level")
    public ResponseEntity<ExpenseResponseDTO> rejectFirstLevel(
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
        Expense current = expenseService.getExpenseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.EXPENSE, 1);
        Expense expense = expenseService.rejectFirstLevel(id, finalApproverId, rejectionComments);
        expense.setDocumentUrls(
            expense.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(expense));
    }

    @PutMapping("/{id}/reject-second-level")
    public ResponseEntity<ExpenseResponseDTO> rejectSecondLevel(
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
        Expense current = expenseService.getExpenseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        approvalHierarchyService.assertActorCanApprove(actor, current.getEmployee(), ApprovalModule.EXPENSE, 2);
        Expense expense = expenseService.rejectSecondLevel(id, finalApproverId, rejectionComments);
        expense.setDocumentUrls(
            expense.getDocumentUrls().stream()
                .map(this::convertToPresignedUrl)
                .collect(Collectors.toList())
        );
        return ResponseEntity.ok(ExpenseResponseDTO.fromExpense(expense));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ExpenseResponseDTO>> getPendingExpenses(
            @RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (employeeId != null) {
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        }
        List<Expense> pendingFirstLevel = (employeeId == null)
            ? expenseService.getExpensesByFirstLevelApprovalStatus(ExpenseStatus.PENDING)
            : expenseService.getExpensesByFirstLevelApprovalStatusAndEmployeeId(ExpenseStatus.PENDING, employeeId);
        List<Expense> pendingSecondLevel = (employeeId == null)
            ? expenseService.getPendingSecondLevelExpenses()
            : expenseService.getPendingSecondLevelExpensesByEmployeeId(employeeId);
        // Combine and remove duplicates
        List<Expense> allPending = new java.util.ArrayList<>(pendingFirstLevel);
        for (Expense e : pendingSecondLevel) {
            if (!allPending.contains(e)) allPending.add(e);
        }
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            allPending = allPending.stream()
                    .filter(exp -> exp.getEmployee() != null
                            && exp.getEmployee().getOrganization() != null
                            && actorOrgId.equals(exp.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        allPending = allPending.stream()
                .filter(exp -> isActionableForActor(actor, exp))
                .collect(Collectors.toList());
        List<ExpenseResponseDTO> dtos = allPending.stream()
                .peek(exp -> exp.setDocumentUrls(
                        exp.getDocumentUrls().stream()
                                .map(this::convertToPresignedUrl)
                                .collect(Collectors.toList())
                ))
                .map(ExpenseResponseDTO::fromExpense)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private boolean isActionableForActor(Employee actor, Expense expense) {
        if (expense == null || expense.getEmployee() == null) {
            return false;
        }
        boolean isFirstLevelPending = expense.getFirstLevelApprovalStatus() == ExpenseStatus.PENDING;
        boolean isSecondLevelPending = expense.getFirstLevelApprovalStatus() == ExpenseStatus.APPROVED
                && expense.getSecondLevelApprovalStatus() == ExpenseStatus.PENDING;
        int levelNo = isFirstLevelPending ? 1 : (isSecondLevelPending ? 2 : -1);
        if (levelNo < 0) {
            return false;
        }
        try {
            approvalHierarchyService.assertActorCanApprove(actor, expense.getEmployee(), ApprovalModule.EXPENSE, levelNo);
            return true;
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    @GetMapping("/approved")
    public ResponseEntity<List<ExpenseResponseDTO>> getApprovedExpenses(
            @RequestParam(value = "employeeId", required = false) Long employeeId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (employeeId != null) {
            tenantAccessService.assertCanAccessEmployeeId(actor, employeeId);
        }
        List<Expense> expenses = (employeeId == null)
            ? expenseService.getFullyApprovedExpenses()
            : expenseService.getFullyApprovedExpensesByEmployeeId(employeeId);
        if (!tenantAccessService.isMainAdmin(actor)) {
            Long actorOrgId = tenantAccessService.requireOrganizationId(actor);
            expenses = expenses.stream()
                    .filter(exp -> exp.getEmployee() != null
                            && exp.getEmployee().getOrganization() != null
                            && actorOrgId.equals(exp.getEmployee().getOrganization().getId()))
                    .collect(Collectors.toList());
        }
        List<ExpenseResponseDTO> dtos = expenses.stream()
                .peek(exp -> exp.setDocumentUrls(
                        exp.getDocumentUrls().stream()
                                .map(this::convertToPresignedUrl)
                                .collect(Collectors.toList())
                ))
                .map(ExpenseResponseDTO::fromExpense)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Helper to convert stored S3 key or URL to a presigned download link.
     */
    private String convertToPresignedUrl(String s3Url) {
        try {
            if (s3Url.startsWith("http")) {
                URI uri = new URI(s3Url);
                String path = uri.getPath();
                return s3Service.getPresignedUrl(path.startsWith("/") ? path.substring(1) : path);
            }
            return s3Service.getPresignedUrl(s3Url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid S3 URL format: " + s3Url, e);
        }
    }
}
