package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.Expense;
import com.zenyrahr.hrms.model.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDTO {
    private Long id;
    private LocalDate date;
    private String category;
    private Float amount;
    private String description;
    private ExpenseStatus firstLevelApprovalStatus;
    private ExpenseStatus secondLevelApprovalStatus;
    private String firstLevelApprover;
    private String secondLevelApprover;
    private String approvalComments1;
    private String approvalComments2;
    private LocalDate firstLevelApprovalDate;
    private LocalDate secondLevelApprovalDate;
    private String rejectionComments;
    private List<String> documentUrls;
    private EmployeeSummaryDTO employee;
    
    public static ExpenseResponseDTO fromExpense(Expense expense) {
        ExpenseResponseDTO dto = new ExpenseResponseDTO();
        dto.setId(expense.getId());
        dto.setDate(expense.getDate());
        dto.setCategory(expense.getCategory());
        dto.setAmount(expense.getAmount());
        dto.setDescription(expense.getDescription());
        dto.setFirstLevelApprovalStatus(expense.getFirstLevelApprovalStatus());
        dto.setSecondLevelApprovalStatus(expense.getSecondLevelApprovalStatus());
        dto.setFirstLevelApprover(expense.getFirstLevelApprover());
        dto.setSecondLevelApprover(expense.getSecondLevelApprover());
        dto.setApprovalComments1(expense.getApprovalComments1());
        dto.setApprovalComments2(expense.getApprovalComments2());
        dto.setFirstLevelApprovalDate(expense.getFirstLevelApprovalDate());
        dto.setSecondLevelApprovalDate(expense.getSecondLevelApprovalDate());
        dto.setRejectionComments(expense.getRejectionComments());
        dto.setDocumentUrls(expense.getDocumentUrls());
        
        if (expense.getEmployee() != null) {
            EmployeeSummaryDTO employeeSummary = new EmployeeSummaryDTO();
            employeeSummary.setName(expense.getEmployee().getFirstName() + " " + expense.getEmployee().getLastName());
            employeeSummary.setRole(expense.getEmployee().getRole());
            employeeSummary.setDepartment(expense.getEmployee().getDepartment());
            dto.setEmployee(employeeSummary);
        }
        
        return dto;
    }
} 