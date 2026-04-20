package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.TravelRequest;
import com.zenyrahr.hrms.model.TravelStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface TravelRequestService {
    TravelRequest saveTravelRequest(TravelRequest travelRequest);

    List<TravelRequest> getAllTravelRequests();

    Optional<TravelRequest> getTravelRequestById(Long id);

    List<TravelRequest> getTravelRequestByEmployeeId(Long EmployeeId);

    TravelRequest updateTravelRequest(Long id, TravelRequest travelRequestDetails);

    void deleteTravelRequest(Long id);

    Optional<TravelRequest> updateTravelRequestStatus(Long id, TravelStatus status);

    TravelRequest approveFirstLevel(Long id, Long approverId, String approvalComments1);

    // First-level rejection
    TravelRequest rejectFirstLevel(Long id, Long approverId, String rejectionComments);

    // Second-level approval
    TravelRequest approveSecondLevel(Long expenseId, Long approverId, String approvalComments2);

    // Second-level rejection
    TravelRequest rejectSecondLevel(Long expenseId, Long approverId, String rejectionComments);

    TravelRequest uploadDocuments(Long travelRequestId, List<MultipartFile> files) throws IOException;

    List<TravelRequest> getTravelRequestsByFirstLevelApprovalStatus(TravelStatus status);
    List<TravelRequest> getTravelRequestsByFirstLevelApprovalStatusAndEmployeeId(TravelStatus status, Long employeeId);

    // New methods for two-level approval
    List<TravelRequest> getFullyApprovedTravelRequests();
    List<TravelRequest> getFullyApprovedTravelRequestsByEmployeeId(Long employeeId);
    List<TravelRequest> getPendingSecondLevelTravelRequests();
    List<TravelRequest> getPendingSecondLevelTravelRequestsByEmployeeId(Long employeeId);
}
