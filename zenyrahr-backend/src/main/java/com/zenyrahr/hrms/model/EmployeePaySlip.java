package com.zenyrahr.hrms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePaySlip {

    private String logoPath;
    private String companyName;
    private String companyAddress;

    private String employeeName;
    private String payYear;
    private String employeeId;
    private String department;
    private String designation;
    private String dateOfJoining;
    private String mailId;

    private String bankName;
    private String accountNumber;
    private String pan;
    private String uan;
    private String pfNumber;
    private String esiNumber;

    private String payMonth;
    private String startDate;
    private String endDate;
    private String workingDays;
    private String paidDays;
    private String lopDays;

    private String grossPay;
    private String netPay;
    private String netPayInWords;
    private String totalDeductions;
    private String totalEarnings;


    private String basicPay;
    private String houseRentAllowance;
    /** Used for payroll-based slips; Excel import may leave at 0. */
    private String dearnessAllowance;
    private String conveyanceAllowance;
    private String medicalAllowance;
    private String otherAllowances;

    private String epfAmount;
    private String professionalTax;
    private String healthInsuranceDeduction;
}

