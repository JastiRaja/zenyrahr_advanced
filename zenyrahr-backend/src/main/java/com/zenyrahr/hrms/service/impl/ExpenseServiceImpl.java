//package com.talvox.hrms.service.impl;
//
//import com.talvox.hrms.model.Expense;
//import com.talvox.hrms.model.ExpenseStatus;
//import com.talvox.hrms.Repository.ExpenseRepository;
//import com.talvox.hrms.service.ExpenseService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class ExpenseServiceImpl implements ExpenseService {
//
//    @Autowired
//    private ExpenseRepository expenseRepository;
//
//    @Override
//    public Expense saveExpense(Expense expense) {
//        expense.setStatus(ExpenseStatus.PENDING);
//
//        return expenseRepository.save(expense);
//    }
//
//    @Override
//    public List<Expense> getAllExpenses() {
//        return expenseRepository.findAll();
//    }
//
//    @Override
//    public Optional<Expense> getExpenseById(Long id) {
//        return expenseRepository.findById(id);
//    }
//
//    @Override
//    public Expense updateExpense(Long id, Expense expenseDetails) {
//        Expense existingExpense = expenseRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Expense not found"));
//
//        existingExpense.setDate(expense
//        Details.getDate());
//        existingExpense.setCategory(expenseDetails.getCategory());
//        existingExpense.setAmount(expenseDetails.getAmount());
//        existingExpense.setDescription(expenseDetails.getDescription());
//        existingExpense.setStatus(expenseDetails.getStatus());
//
//        return expenseRepository.save(existingExpense);
//    }
//
//    @Override
//    public void deleteExpense(Long id) {
//        Expense existingExpense = expenseRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Expense not found"));
//        expenseRepository.delete(existingExpense);
//    }
//
//    @Override
//    public Optional<Expense> updateExpenseStatus(Long id, ExpenseStatus status) {
//        Expense existingExpense = expenseRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Expense not found"));
//        existingExpense.setStatus(status);
//        return Optional.of(expenseRepository.save(existingExpense));
//    }
//    @Override
//    public Optional<Expense> approveExpense(Long id) {
//        Expense existingExpense = expenseRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Expense Request not found"));
//        Expense.setStatus(ExpenseStatus.APPROVED);
//        return Optional.of(expenseRepository.save(existingExpense));
//    }
//
//    @Override
//    public Optional<Expense> rejectExpense(Long id) {
//        Expense existingExpense = expenseRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Expense Request not found"));
//        Expense.setStatus(ExpenseStatus.REJECTED);
//        return Optional.of(expenseRepository.save(existingExpense));
//    }
//}


package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.*;
import com.zenyrahr.hrms.Repository.ExpenseRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Expense;
import com.zenyrahr.hrms.model.ExpenseStatus;
import com.zenyrahr.hrms.service.ExpenseService;
import com.zenyrahr.hrms.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @Override
    public Expense saveExpense(Expense expense) {
        expense.setFirstLevelApprovalStatus(ExpenseStatus.PENDING);
        expense.setSecondLevelApprovalStatus(ExpenseStatus.PENDING);
        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @Override
    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }
    @Override
    public List<Expense> getExpenseByEmployeeId(Long EmployeeId){
        return expenseRepository.findByEmployee_Id(EmployeeId);
    }
    @Override
    public Expense updateExpense(Long id, Expense expenseDetails) {
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        existingExpense.setDate(expenseDetails.getDate());
        existingExpense.setCategory(expenseDetails.getCategory());
        existingExpense.setAmount(expenseDetails.getAmount());
        existingExpense.setDescription(expenseDetails.getDescription());

        return expenseRepository.save(existingExpense);
    }

    @Override
    public void deleteExpense(Long id) {
        Optional<Expense> expenseOptional =expenseRepository.findById(id);

        if (expenseOptional.isPresent()) {
            Expense expense = expenseOptional.get();

            // Assuming expensestatus is an enum and PENDING is one of its constants
            if (ExpenseStatus.PENDING.equals(expense.getFirstLevelApprovalStatus()) &&
                    ExpenseStatus.PENDING.equals(expense.getSecondLevelApprovalStatus())) {
                // If both approval statuses are pending, allow deletion
                expenseRepository.delete(expense);
            } else {
                // If either status is not pending, disallow deletion
                throw new IllegalStateException("Cannot delete request: It has already been approved or rejected.");
            }
        } else {
            // Throw an exception if the expense does not exist
            throw new IllegalArgumentException("Travel request not found with ID: " + id);
        }
    }


    @Override
    public Optional<Expense> updateExpenseStatus(Long id, ExpenseStatus status) {
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Set the new status and save the update
        existingExpense.setFirstLevelApprovalStatus(status);
        return Optional.of(expenseRepository.save(existingExpense));
    }
//
//    @Override
//    public Optional<Expense> approveExpense(Long id) {
//        return Optional.empty();
//    }
//
//    @Override
//    public Optional<Expense> rejectExpense(Long id) {
//        return Optional.empty();
//    }

    // First-level approval
    @Override
    public Expense approveFirstLevel(Long id, Long approverId, String approvalComments1) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        expense.setFirstLevelApprovalStatus(ExpenseStatus.APPROVED);
        expense.setFirstLevelApprover(approverEmployee.getUsername());
        expense.setApprovalComments1(approvalComments1);
        expense.setFirstLevelApprovalDate(LocalDate.now());

        Expense saved = expenseRepository.save(expense);
        return saved;
    }

    // First-level rejection
    @Override
    public Expense rejectFirstLevel(Long id, Long approverId, String rejectionComments) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        expense.setFirstLevelApprovalStatus(ExpenseStatus.REJECTED);
        expense.setFirstLevelApprover(approverEmployee.getUsername());
        expense.setRejectionComments(rejectionComments);
        expense.setFirstLevelApprovalDate(LocalDate.now());

        return expenseRepository.save(expense);
    }

    // Second-level approval
    @Override
    public Expense approveSecondLevel(Long id, Long approverId, String approvalComments2) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (expense.getFirstLevelApprovalStatus() != ExpenseStatus.APPROVED) {
            throw new IllegalStateException("First-level approval required before second-level approval.");
        }

        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        expense.setSecondLevelApprovalStatus(ExpenseStatus.APPROVED);
        expense.setSecondLevelApprover(approverEmployee.getUsername());
        expense.setApprovalComments2(approvalComments2);
        expense.setSecondLevelApprovalDate(LocalDate.now());

        Expense saved = expenseRepository.save(expense);
        return saved;
    }

    // Second-level rejection
    @Override
    public Expense rejectSecondLevel(Long id, Long approverId, String rejectionComments) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (expense.getFirstLevelApprovalStatus() != ExpenseStatus.APPROVED) {
            throw new IllegalStateException("First-level approval required before second-level rejection.");
        }

        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        expense.setSecondLevelApprovalStatus(ExpenseStatus.REJECTED);
        expense.setSecondLevelApprover(approverEmployee.getUsername());
        expense.setRejectionComments(rejectionComments);
        expense.setSecondLevelApprovalDate(LocalDate.now());

        return expenseRepository.save(expense);
    }
    @Override
    public Expense uploadDocuments(Long expenseId, List<MultipartFile> files) throws IOException {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Employee employee = expense.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Expense must be linked to an employee");
        }
        String userId = employee.getId().toString();

        String documentUrl = s3Service.uploadCategoryDocuments(files, userId, "expenses");
        expense.getDocumentUrls().add(documentUrl);

        return expenseRepository.save(expense);
    }

    @Override
    public List<Expense> getExpensesByFirstLevelApprovalStatus(ExpenseStatus status) {
        return expenseRepository.findByFirstLevelApprovalStatus(status);
    }

    @Override
    public List<Expense> getExpensesByFirstLevelApprovalStatusAndEmployeeId(ExpenseStatus status, Long employeeId) {
        return expenseRepository.findByFirstLevelApprovalStatusAndEmployee_Id(status, employeeId);
    }

    // New methods for two-level approval
    @Override
    public List<Expense> getFullyApprovedExpenses() {
        return expenseRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(ExpenseStatus.APPROVED, ExpenseStatus.APPROVED);
    }

    @Override
    public List<Expense> getFullyApprovedExpensesByEmployeeId(Long employeeId) {
        return expenseRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(ExpenseStatus.APPROVED, ExpenseStatus.APPROVED, employeeId);
    }

    @Override
    public List<Expense> getPendingSecondLevelExpenses() {
        return expenseRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(ExpenseStatus.APPROVED, ExpenseStatus.PENDING);
    }

    @Override
    public List<Expense> getPendingSecondLevelExpensesByEmployeeId(Long employeeId) {
        return expenseRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(ExpenseStatus.APPROVED, ExpenseStatus.PENDING, employeeId);
    }
}

