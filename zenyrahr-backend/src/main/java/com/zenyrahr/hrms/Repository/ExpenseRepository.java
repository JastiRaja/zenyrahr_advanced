package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Expense;
import com.zenyrahr.hrms.model.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByEmployee_Id(Long employeeId);
    List<Expense> findByFirstLevelApprovalStatus(ExpenseStatus status);
    List<Expense> findBySecondLevelApprovalStatus(ExpenseStatus status);
    List<Expense> findByFirstLevelApprovalStatusAndEmployee_Id(ExpenseStatus status, Long employeeId);
    List<Expense> findBySecondLevelApprovalStatusAndEmployee_Id(ExpenseStatus status, Long employeeId);
    List<Expense> findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(ExpenseStatus firstLevelStatus, ExpenseStatus secondLevelStatus);
    List<Expense> findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(ExpenseStatus firstLevelStatus, ExpenseStatus secondLevelStatus, Long employeeId);
}
