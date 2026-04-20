package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.EmployeeLeaveBalanceRepository;
import com.zenyrahr.hrms.Repository.AttendanceRepository;
import com.zenyrahr.hrms.Repository.LeaveRequestRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Attendance;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.EmployeeLeaveBalance;
import com.zenyrahr.hrms.model.LeaveRequest;
import com.zenyrahr.hrms.model.LeaveStatus;
import com.zenyrahr.hrms.model.LeaveType;
import com.zenyrahr.hrms.service.LeaveRequestService;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import sibApi.TransactionalEmailsApi;
import sibModel.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;

@RequiredArgsConstructor
@Service
@Slf4j
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SequenceService sequenceService;
    private final S3Service s3Service;

    @Value("${brevo.api.key}")
    private String brevoApiKey;
    
    @Value("${brevo.sender.email}")
    private String brevoSenderEmail;
    
    @Value("${brevo.sender.name}")
    private String brevoSenderName;

    private String buildApprovalEmailContent(String employeeName, LocalDate startDate, LocalDate endDate) {
        return "Dear " + employeeName + ",\n\n" +
                "Your leave request for the period " + startDate + " to " + endDate + " has been approved.\n\n" +
                "Wishing you a pleasant time during your leave.\n\n" +
                "Best regards,\n" +
                "HR Team";
    }

    private String buildRejectEmailContent(String employeeName, LocalDate startDate, LocalDate endDate, String comments) {
        return "Dear " + employeeName + ",\n\n" +
                "Your leave request for the period " + startDate + " to " + endDate + " has been rejected.\n\n" +
                "Reason: " + comments + "\n\n" +
                "Best regards,\n" +
                "HR Team";
    }

    private String buildWithdrawalEmailContent(String employeeName, LocalDate startDate, LocalDate endDate) {
        return "Dear " + employeeName + ",\n\n" +
                "Your leave request for the period " + startDate + " to " + endDate + " has been WithDraw.\n\n" +
                "Reason: " + "\n\n" +
                "Best regards,\n" +
                "HR Team";
    }

    private void sendEmail(String email, String subject, String content) throws Exception {
        // Configure Brevo API client
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        
        // Create the email
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().email(brevoSenderEmail).name(brevoSenderName));
        sendSmtpEmail.setTo(java.util.Arrays.asList(new SendSmtpEmailTo().email(email)));
        sendSmtpEmail.setSubject(subject);
        sendSmtpEmail.setHtmlContent("<html><body>" + content.replace("\n", "<br>") + "</body></html>");
        
        // Send the email
        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("Email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Error sending email via Brevo", e);
            throw new Exception("Error sending email via Brevo", e);
        }
    }

    @Override
    public LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest) {
        // Set the initial status to PENDING
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setRevocationRequested(false);
        leaveRequest.setCode(sequenceService.getNextCode("LR"));

        // Save the leave request to the database
        LeaveRequest savedLeaveRequest = leaveRequestRepository.save(leaveRequest);

        // Fetch the employee details to get the reporting manager
        Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + leaveRequest.getEmployee().getId()));

        // Check if the employee has a reporting manager
        Employee reportingManager = employee.getReportingManager();
        if (reportingManager != null) {
            try {
                // Log reporting manager info (optional)
                log.info("Reporting Manager for leave request {} is {}", savedLeaveRequest.getId(), reportingManager.getUsername());

                // Prepare the email content
                String emailContent = "Dear " + reportingManager.getUsername() + ",\n\n" +
                        "A new leave request has been created by " + employee.getName() +
                        " for the period " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + ".\n\n" +
                        "Comments: " + (leaveRequest.getComments() != null ? leaveRequest.getComments() : "No comments provided.") + "\n\n" +
                        "Please review and take necessary action.\n\n" +
                        "Best regards,\n" +
                        "HR Team";

                // Send the email to the manager
                sendEmail(reportingManager.getUsername(), "New Leave Request Notification", emailContent);
            } catch (Exception e) {
                // Log the exception without blocking leave request creation
                log.warn("Failed to send leave request notification to reporting manager {}", reportingManager.getUsername(), e);
            }
        } else {
            log.info("Employee {} does not have a reporting manager configured", employee.getName());
        }

        // Return the saved leave request
        return savedLeaveRequest;
    }


    @Override
    public List<LeaveRequest> getLeaveRequestsByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployee_Id(employeeId);
    }

    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    @Override
    public LeaveRequest approveLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED) {
            throw new RuntimeException("Cannot approve a canceled leave request.");
        }

        Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveType leaveType = leaveRequest.getLeaveType();
        EmployeeLeaveBalance employeeLeaveBalance = employeeLeaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        long requestedDays = leaveRequest.getEndDate().toEpochDay() - leaveRequest.getStartDate().toEpochDay() + 1;

        if (employeeLeaveBalance.getBalance() >= requestedDays) {
            employeeLeaveBalance.setBalance((int) (employeeLeaveBalance.getBalance() - requestedDays));
            employeeLeaveBalanceRepository.save(employeeLeaveBalance);

            try {
                sendEmail(
                        employee.getUsername(),
                        "Leave Request Approved",
                        buildApprovalEmailContent(employee.getName(), leaveRequest.getStartDate(), leaveRequest.getEndDate())
                );
            } catch (Exception emailError) {
                log.error("Failed to send approval email for leave request {}", id, emailError);
            }

            leaveRequest.setStatus(LeaveStatus.APPROVED);
            leaveRequest.setRevocationRequested(false);
            LeaveRequest approved = leaveRequestRepository.save(leaveRequest);
            applyApprovedLeaveToAttendance(approved);
            return approved;
        } else {
            throw new RuntimeException("Insufficient leave balance.");
        }
    }



    @Override
    public LeaveRequest rejectLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED) {
            throw new RuntimeException("Cannot reject a canceled leave request.");
        }

        Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveType leaveType = leaveRequest.getLeaveType();
        EmployeeLeaveBalance employeeLeaveBalance = employeeLeaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        long requestedDays = leaveRequest.getEndDate().toEpochDay() - leaveRequest.getStartDate().toEpochDay() + 1;
        employeeLeaveBalance.setBalance((int) (employeeLeaveBalance.getBalance() + requestedDays));
        employeeLeaveBalanceRepository.save(employeeLeaveBalance);

        try {
            sendEmail(
                    employee.getUsername(),
                    "Leave Request Rejected",
                    buildRejectEmailContent(employee.getName(), leaveRequest.getStartDate(), leaveRequest.getEndDate(), leaveRequest.getComments())
            );
        } catch (Exception emailError) {
            log.error("Failed to send rejection email for leave request {}", id, emailError);
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest withdrawrejectLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        // Only allow withdrawal if status is PENDING
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Cannot withdraw a leave request that is not pending.");
        }

        Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Send notification to employee about withdrawal
        try {
            sendEmail(
                    employee.getUsername(),
                    "Leave Request Withdrawn",
                    buildWithdrawalEmailContent(employee.getName(), leaveRequest.getStartDate(), leaveRequest.getEndDate())
            );
        } catch (Exception emailError) {
            log.error("Failed to send withdrawal email for leave request {}", id, emailError);
        }

        // Update leave request status to WITHDRAWN
        leaveRequest.setStatus(LeaveStatus.WITHDRAWN);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest requestRevokeLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.APPROVED) {
            throw new RuntimeException("Only approved leave requests can be revoked.");
        }
        leaveRequest.setRevocationRequested(true);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest approveRevokeLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.APPROVED || !Boolean.TRUE.equals(leaveRequest.getRevocationRequested())) {
            throw new RuntimeException("Only revocation pending leave requests can be approved.");
        }

        Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveType leaveType = leaveRequest.getLeaveType();
        EmployeeLeaveBalance employeeLeaveBalance = employeeLeaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        long requestedDays = leaveRequest.getEndDate().toEpochDay() - leaveRequest.getStartDate().toEpochDay() + 1;
        employeeLeaveBalance.setBalance((int) (employeeLeaveBalance.getBalance() + requestedDays));
        employeeLeaveBalanceRepository.save(employeeLeaveBalance);

        leaveRequest.setStatus(LeaveStatus.WITHDRAWN);
        leaveRequest.setRevocationRequested(false);
        LeaveRequest withdrawn = leaveRequestRepository.save(leaveRequest);
        revertLeaveAttendance(withdrawn);
        return withdrawn;
    }

    @Override
    public LeaveRequest rejectRevokeLeaveRequest(Long id) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.APPROVED || !Boolean.TRUE.equals(leaveRequest.getRevocationRequested())) {
            throw new RuntimeException("Only revocation pending leave requests can be rejected.");
        }
        leaveRequest.setRevocationRequested(false);
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        return leaveRequestRepository.save(leaveRequest);
    }


    @Override
    public LeaveRequest cancelLeaveRequest(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            Employee employee = userRepository.findById(leaveRequest.getEmployee().getId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            LeaveType leaveType = leaveRequest.getLeaveType();
            EmployeeLeaveBalance employeeLeaveBalance = employeeLeaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            long requestedDays = leaveRequest.getEndDate().toEpochDay() - leaveRequest.getStartDate().toEpochDay() + 1;
            employeeLeaveBalance.setBalance((int) (employeeLeaveBalance.getBalance() + requestedDays));
            employeeLeaveBalanceRepository.save(employeeLeaveBalance);
        }
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        return leaveRequestRepository.save(leaveRequest);
    }

    private void applyApprovedLeaveToAttendance(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getId() == null) {
            return;
        }
        Long employeeId = leaveRequest.getEmployee().getId();
        for (LocalDate date = leaveRequest.getStartDate(); !date.isAfter(leaveRequest.getEndDate()); date = date.plusDays(1)) {
            Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
            Attendance attendance;
            if (existing.isPresent()) {
                attendance = existing.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(leaveRequest.getEmployee());
                attendance.setDate(date);
            }

            String currentStatus = attendance.getStatus();
            if ("HOLIDAY".equalsIgnoreCase(currentStatus) ||
                    "PRESENT".equalsIgnoreCase(currentStatus) ||
                    "HALF_DAY".equalsIgnoreCase(currentStatus) ||
                    "CHECKED_IN".equalsIgnoreCase(currentStatus)) {
                continue;
            }

            attendance.setStatus("LEAVE");
            attendance.setRemarks("Approved leave");
            attendanceRepository.save(attendance);
        }
    }

    private void revertLeaveAttendance(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployee() == null || leaveRequest.getEmployee().getId() == null) {
            return;
        }
        Long employeeId = leaveRequest.getEmployee().getId();
        for (LocalDate date = leaveRequest.getStartDate(); !date.isAfter(leaveRequest.getEndDate()); date = date.plusDays(1)) {
            Optional<Attendance> attendanceOptional = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
            if (attendanceOptional.isEmpty()) {
                continue;
            }
            Attendance attendance = attendanceOptional.get();
            if (attendance.getCheckInTime() != null) {
                if (attendance.getCheckOutTime() != null) {
                    long minutesWorked = java.time.Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
                    attendance.setStatus(minutesWorked >= 8 * 60 ? "PRESENT" : "HALF_DAY");
                } else {
                    attendance.setStatus("CHECKED_IN");
                }
            } else {
                attendance.setStatus("ABSENT");
            }
            if ("Approved leave".equalsIgnoreCase(String.valueOf(attendance.getRemarks()))) {
                attendance.setRemarks("Leave revoked");
            }
            attendanceRepository.save(attendance);
        }
    }

    private void validateLeaveDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date.");
        }
    }

    @Override
    public LeaveRequest uploadDocuments(Long leaveRequestId, List<MultipartFile> files) throws IOException {
        // Retrieve the LeaveRequest from the repository.
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("LeaveRequest not found"));

        // Ensure the LeaveRequest is associated with an Employee.
        Employee employee = leaveRequest.getEmployee();
        if (employee == null) {
            throw new RuntimeException("LeaveRequest must be associated with an employee");
        }
        String userId = employee.getId().toString();

        // Upload files to S3 and get the document URL.
        String documentUrl = s3Service.uploadCategoryDocuments(files, userId, "leaveRequest");
        log.info("Uploaded document URL: {}", documentUrl);

        // Add the document URL to the LeaveRequest's documentUrls list.
        leaveRequest.getDocumentUrls().add(documentUrl);

        // Save the updated LeaveRequest and log the updated URLs.
        LeaveRequest updatedTicket = leaveRequestRepository.save(leaveRequest);
        log.info("Updated LeaveRequest document URLs: {}", updatedTicket.getDocumentUrls());

        return updatedTicket;
    }

    @Override
    public LeaveRequest updateLeaveRequest(Long id, LeaveRequest leaveRequest) {
        if (leaveRequestRepository.existsById(id)) {
            leaveRequest.setId(id);
            return leaveRequestRepository.save(leaveRequest);
        } else {
            throw new RuntimeException("LeaveRequest not found");
        }
    }


    @Override
    public Optional<LeaveRequest> getLeaveRequestById(Long Id) {
        return leaveRequestRepository.findById(Id);
    }

    @Override
    public List<LeaveRequest> getPendingLeaveRequestsByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployee_IdAndStatus(employeeId, LeaveStatus.PENDING);
    }
}