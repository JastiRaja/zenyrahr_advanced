package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.LeaveRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestService {
    LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest);

    List<LeaveRequest> getLeaveRequestsByEmployee(Long employeeId);

    LeaveRequest approveLeaveRequest(Long id) throws Exception;

    LeaveRequest rejectLeaveRequest(Long id) throws Exception;

    LeaveRequest withdrawrejectLeaveRequest(Long id) throws Exception;
    LeaveRequest requestRevokeLeaveRequest(Long id) throws Exception;
    LeaveRequest approveRevokeLeaveRequest(Long id) throws Exception;
    LeaveRequest rejectRevokeLeaveRequest(Long id) throws Exception;

    List<LeaveRequest> getAllLeaveRequests();

    LeaveRequest cancelLeaveRequest(Long id);

    LeaveRequest updateLeaveRequest(Long id, LeaveRequest leaveRequest);

    LeaveRequest uploadDocuments(Long leaveRequestId, List<MultipartFile> files) throws IOException;

    Optional<LeaveRequest> getLeaveRequestById(Long id);

    List<LeaveRequest> getPendingLeaveRequestsByEmployee(Long employeeId);

}

