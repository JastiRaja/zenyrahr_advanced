package com.zenyrahr.hrms.service;

import com.lowagie.text.Row;

import com.zenyrahr.hrms.model.EmployeePaySlip;
import com.zenyrahr.hrms.utils.DateUtils;
import com.zenyrahr.hrms.utils.JasperReport;
import com.zenyrahr.hrms.utils.NumberToWordsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sibApi.TransactionalEmailsApi;
import sibModel.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReaderService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;
    
    @Value("${brevo.sender.email}")
    private String brevoSenderEmail;
    
    @Value("${brevo.sender.name}")
    private String brevoSenderName;
    
    private final JasperReport jasperReport;
    public byte[] readExcelFile(MultipartFile file) throws JRException, IOException {
        // Load the Excel file
        Workbook workbook ;
        try (InputStream inputStream = file.getInputStream()) {
            // Assuming it's an Excel file (.xlsx), you can handle .xls by changing to HSSFWorkbook
            workbook =  new XSSFWorkbook(inputStream);
        }
        Sheet sheet = workbook.getSheetAt(0); // Read the first sheet

        // Get the constant values from the second row (index 1)

        Row secondRow = (Row) sheet.getRow(0);

        String payMonth = getCellValue(secondRow, 2);  // Assuming column 2 for pay month
        String startDate = DateUtils.getFirstDateOfMonth(payMonth, 2024); // Assuming column 0 for start date
        String endDate = DateUtils.getLastDateOfMonth(payMonth, 2024);;   // Assuming column 1 for end date
        String companyName = getCellValue(secondRow, 0); // Assuming column 3 for company name
        String companyAddress = getCellValue(secondRow, 1); // Assuming column 4 for company address
        String workingDays = getCellValue(secondRow,5);
        // List to store multiple EmployeePaySlip objects
        List<EmployeePaySlip> employees = new ArrayList<>();

        // Loop through the rows starting from the third row (index 2) for employee data
        for (int rowIndex = 2; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
            Row row = (Row) sheet.getRow(rowIndex); // Employee data starts from the third row (index 2)
            EmployeePaySlip employee = calculateSalary(Double.parseDouble(getCellValue(row,12)));

            // Employee-specific fields
            employee.setPayYear("2024");
            employee.setEmployeeName(getCellValue(row, 0));  // Column 0 for Employee Name
            employee.setMailId(getCellValue(row, 1));        // Column 1 for Mail Id
            employee.setEmployeeId(getCellValue(row, 2));    // Column 2 for Employee ID
            employee.setDesignation(getCellValue(row, 3));   // Column 3 for Designation
            employee.setDepartment(getCellValue(row, 4));    // Column 4 for Department
            employee.setWorkingDays(workingDays);
            employee.setBankName(getCellValue(row, 5));      // Column 5 for Bank Name
            employee.setAccountNumber(getCellValue(row, 6)); // Column 6 for Account Number
            employee.setDateOfJoining(getCellValue(row, 7)); // Column 7 for Date of Joining
            employee.setPaidDays(getCellValue(row, 8));      // Column 8 for Paid Days

            employee.setLopDays(getCellValue(row, 9));       // Column 9 for LOP Days

            employee.setPan(getCellValue(row, 10));           // Column 10 for PAN
            employee.setUan(getCellValue(row, 11));          // Column 11 for UAN

            // Set the same values for all employees (fetched from second row)
            employee.setStartDate(startDate);
            employee.setEndDate(endDate);
            employee.setPayMonth(payMonth);
            employee.setCompanyName(companyName);
            employee.setCompanyAddress(companyAddress);

            // Add employee to the list
            employees.add(employee);
        }

        // Close the file input stream
        workbook.close();

        // Compile the Jasper report
        String sourceFileName = "src/main/resources/reports/payslip.jrxml";
        String compiledFileName = "src/main/resources/reports/payslip.jasper";
        JasperCompileManager.compileReportToFile(sourceFileName, compiledFileName);
        String jasperFile = "src/main/resources/reports/payslip.jasper";

        // Prepare the data for the report
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(employees);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Monthly Payslip");

        // Fill the report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperFile, parameters, dataSource);
        ByteArrayOutputStream pdfStream = jasperReport.exportToPdfStream(jasperPrint);

        // Export the report to PDF
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);

        // Send email to each employee using Brevo
        sendEmailToEmployees(employees, pdfStream.toByteArray());

        return pdfStream.toByteArray();
    }

    // Helper method to get cell value from Excel (returns String by default)
    private String getCellValue(Row row, int columnIndex) {
        Cell cell = (Cell) row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Check if the number is an integer or large number
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    // If the cell is a date, handle it as a date
                    return sdf.format(cell.getDateCellValue()); // or use a simple date format
                } else {
                    // For large numbers, we format as string to prevent scientific notation
                    return formatLargeNumber(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    // Helper method to handle large numbers as strings
    private String formatLargeNumber(double value) {
        // Convert the double to string directly
        // If it's a long number, we should treat it as string to avoid scientific notation
        BigDecimal bigDecimal = new BigDecimal(value);
        return bigDecimal.toPlainString();  // Avoids scientific notation and keeps full number
    }
    // Method to send email using Brevo
    private void sendEmailToEmployees(List<EmployeePaySlip> employees, byte[] pdfContent) {
        for (EmployeePaySlip employee : employees) {
            try {
                // Configure Brevo API client
                ApiClient defaultClient = Configuration.getDefaultApiClient();
                ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
                apiKey.setApiKey(brevoApiKey);
                TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
                
                // Create email content
                String emailContent = "Dear " + employee.getEmployeeName() + ",\n\nPlease find attached your monthly payslip.\n\nBest regards,\nZenyraHR";
                
                // Create the email
                SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
                sendSmtpEmail.setSender(new SendSmtpEmailSender().email(brevoSenderEmail).name(brevoSenderName));
                sendSmtpEmail.setTo(java.util.Arrays.asList(new SendSmtpEmailTo().email(employee.getMailId()).name(employee.getEmployeeName())));
                sendSmtpEmail.setSubject("Your Monthly Payslip");
                sendSmtpEmail.setHtmlContent("<html><body>" + emailContent.replace("\n", "<br>") + "</body></html>");
                
                // Add attachment
                SendSmtpEmailAttachment attachment = new SendSmtpEmailAttachment();
                attachment.setContent(Base64.getEncoder().encodeToString(pdfContent).getBytes());
                attachment.setName("Payslip_" + employee.getEmployeeName() + ".pdf");
                sendSmtpEmail.setAttachment(java.util.Arrays.asList(attachment));
                
                // Send the email
                apiInstance.sendTransacEmail(sendSmtpEmail);
            } catch (Exception e) {
                log.error("Failed to send payslip email to {}", employee.getMailId(), e);
            }
        }
    }


    public EmployeePaySlip calculateSalary(double grossEarnings) {

        EmployeePaySlip employeePaySlip = new EmployeePaySlip();
        employeePaySlip.setGrossPay(String.valueOf(grossEarnings));
        // Basic Wage Calculation
        double basicWages = roundToTwoDecimalPlaces(grossEarnings / 3);
        employeePaySlip.setBasicPay(String.valueOf(basicWages));
        // HRA (40% of Basic)
        double hra = roundToTwoDecimalPlaces(basicWages * 0.40);
        employeePaySlip.setHouseRentAllowance(String.valueOf(hra));
        employeePaySlip.setDearnessAllowance("0.00");
        // Conveyance (75% of Basic)
        double conveyance = roundToTwoDecimalPlaces(basicWages * 0.75);
        employeePaySlip.setConveyanceAllowance(String.valueOf(conveyance));
        // Medical (fixed value)
        double medical = 1250.00;
        employeePaySlip.setHealthInsuranceDeduction("0.00");
        employeePaySlip.setMedicalAllowance(String.valueOf(medical));
        // Other Allowances (Gross - basic - HRA - conveyance - medical)
        double otherAllowances = roundToTwoDecimalPlaces(grossEarnings - basicWages - hra - conveyance - medical);
        employeePaySlip.setOtherAllowances(String.valueOf(otherAllowances));
        // EPF (24% of Basic)
        double epf = roundToTwoDecimalPlaces(basicWages * 0.24);
        employeePaySlip.setEpfAmount(String.valueOf(epf));
        double totalEarnings = basicWages + hra + conveyance + medical + otherAllowances;
        employeePaySlip.setTotalEarnings(String.valueOf(totalEarnings));
        double professionalTax = 200.00;
        employeePaySlip.setProfessionalTax(String.valueOf(professionalTax));
        // Total Deductions
        double totalDeductions = epf + professionalTax;
        employeePaySlip.setTotalDeductions(String.valueOf(totalDeductions));
        // Net Amount
        double netAmount = totalEarnings - totalDeductions;
        employeePaySlip.setNetPay(String.valueOf(netAmount));
        employeePaySlip.setNetPayInWords(NumberToWordsConverter.convert(netAmount));
        // Create and return SalaryDetails object
        return employeePaySlip;
    }

    private double roundToTwoDecimalPlaces(double value) {
        BigDecimal bd = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
