package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Payroll;
import com.zenyrahr.hrms.model.PayrollGeneration;
import java.time.YearMonth;
import java.util.List;

public interface PayrollService {
    // PayrollGeneration methods
    PayrollGeneration generatePayroll(Long organizationId, String monthYear, String generatedBy);
    PayrollGeneration approvePayroll(Long id, String approvedBy);
    PayrollGeneration rejectPayroll(Long id, String approvedBy, String rejectionReason);
    PayrollGeneration getPayrollGenerationById(Long id);
    List<PayrollGeneration> getAllPayrolls(Long organizationId);
    List<PayrollGeneration> getPayrollsByStatus(Long organizationId, String status);
    List<PayrollGeneration> getPayrollsByMonthYear(Long organizationId, String monthYear);
    void deletePayroll(Long id);

    // Payroll methods
    Payroll generatePayroll(Long employeeId, Payroll payrollData);
    List<Payroll> getPayrollsByEmployeeId(Long employeeId);
    Payroll generatePayrollForEmployee(Long employeeId, YearMonth month);
    List<Payroll> generatePayrollForAllEmployees(YearMonth month);
    Payroll getPayrollById(Long id);
    Payroll save(Payroll payroll);
}
