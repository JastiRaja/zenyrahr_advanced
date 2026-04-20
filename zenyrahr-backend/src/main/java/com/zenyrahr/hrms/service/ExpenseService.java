package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Expense;
import com.zenyrahr.hrms.model.ExpenseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ExpenseService {
    Expense saveExpense(Expense expense);
    List<Expense> getAllExpenses();
    Optional<Expense> getExpenseById(Long id);

    List<Expense> getExpenseByEmployeeId(Long EmployeeId);

    Expense updateExpense(Long id, Expense expenseDetails);
    void deleteExpense(Long id);
    Optional<Expense> updateExpenseStatus(Long id, ExpenseStatus status); // New method for updating status

    // First-level approval
    Expense approveFirstLevel(Long id, Long approverId, String approvalComments1);


    // First-level rejection
    Expense rejectFirstLevel(Long id, Long approverId, String rejectionComments);

    // Second-level approval
    Expense approveSecondLevel(Long id, Long approverId, String approvalComments2);

    // Second-level rejection
    Expense rejectSecondLevel(Long id, Long approverId, String rejectionComments);

    Expense uploadDocuments(Long expenseId, List<MultipartFile> files) throws IOException;

    List<Expense> getExpensesByFirstLevelApprovalStatus(ExpenseStatus status);
    List<Expense> getExpensesByFirstLevelApprovalStatusAndEmployeeId(ExpenseStatus status, Long employeeId);

    // New methods for two-level approval
    List<Expense> getFullyApprovedExpenses();
    List<Expense> getFullyApprovedExpensesByEmployeeId(Long employeeId);
    List<Expense> getPendingSecondLevelExpenses();
    List<Expense> getPendingSecondLevelExpensesByEmployeeId(Long employeeId);
}
