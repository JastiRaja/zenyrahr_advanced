package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.TravelRequestRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.TravelRequest;
import com.zenyrahr.hrms.model.TravelStatus;
import com.zenyrahr.hrms.service.S3Service;
import com.zenyrahr.hrms.service.TravelRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TravelRequestServiceImpl implements TravelRequestService {

    @Autowired
    private TravelRequestRepository travelRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private S3Service s3Service;

    @Override
    public TravelRequest saveTravelRequest(TravelRequest travelRequest) {
        travelRequest.setStatus(TravelStatus.ACTIVE);
        travelRequest.setFirstLevelApprovalStatus(TravelStatus.PENDING);
        travelRequest.setSecondLevelApprovalStatus(TravelStatus.PENDING);
        return travelRequestRepository.save(travelRequest);
    }

    @Override
    public List<TravelRequest> getAllTravelRequests() {

        return travelRequestRepository.findAll();
    }

    @Override
    public Optional<TravelRequest> getTravelRequestById(Long id) {

        return travelRequestRepository.findById(id);
    }
    @Override
    public List<TravelRequest> getTravelRequestByEmployeeId(Long EmployeeId){
        return travelRequestRepository.findByEmployee_Id(EmployeeId);
    }

    @Override
    public TravelRequest updateTravelRequest(Long id, TravelRequest travelRequestDetails) {
        TravelRequest existingTravelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));

        existingTravelRequest.setDestination(travelRequestDetails.getDestination());
        existingTravelRequest.setPurpose(travelRequestDetails.getPurpose());
        existingTravelRequest.setStartDate(travelRequestDetails.getStartDate());
        existingTravelRequest.setEndDate(travelRequestDetails.getEndDate());
        existingTravelRequest.setBudget(travelRequestDetails.getBudget());
        existingTravelRequest.setDescription(travelRequestDetails.getDescription());
        existingTravelRequest.setTransportation(travelRequestDetails.getTransportation());
        existingTravelRequest.setAccommodation(travelRequestDetails.getAccommodation());

        return travelRequestRepository.save(existingTravelRequest);
    }

    @Override
    public void deleteTravelRequest(Long id) {
        Optional<TravelRequest> travelRequestOptional = travelRequestRepository.findById(id);

        if (travelRequestOptional.isPresent()) {
            TravelRequest travelRequest = travelRequestOptional.get();

            // Assuming TravelStatus is an enum and PENDING is one of its constants
            if (TravelStatus.PENDING.equals(travelRequest.getFirstLevelApprovalStatus()) &&
                    TravelStatus.PENDING.equals(travelRequest.getSecondLevelApprovalStatus())) {
                // If both approval statuses are pending, allow deletion
                travelRequestRepository.delete(travelRequest);
            } else {
                // If either status is not pending, disallow deletion
                throw new IllegalStateException("Cannot delete request: It has already been approved or rejected.");
            }
        } else {
            // Throw an exception if the travel request does not exist
            throw new IllegalArgumentException("Travel request not found with ID: " + id);
        }
    }


    @Override
    public Optional<TravelRequest> updateTravelRequestStatus(Long id, TravelStatus status) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));

        travelRequest.setFirstLevelApprovalStatus(status); // Update the status
        return Optional.of(travelRequestRepository.save(travelRequest));
    }

    // First-level approval
    @Override
    public TravelRequest approveFirstLevel(Long id, Long approverId, String approvalComments1) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));
        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        travelRequest.setFirstLevelApprovalStatus(TravelStatus.APPROVED);
        travelRequest.setFirstLevelApprover(approverEmployee.getUsername());
        travelRequest.setApprovalComments1(approvalComments1);
        travelRequest.setFirstLevelApprovalDate(LocalDate.now());
        return travelRequestRepository.save(travelRequest);
    }

    // First-level rejection
    @Override
    public TravelRequest rejectFirstLevel(Long id, Long approverId, String rejectionComments) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));
        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        travelRequest.setFirstLevelApprovalStatus(TravelStatus.REJECTED);
        travelRequest.setFirstLevelApprover(approverEmployee.getUsername());
        travelRequest.setRejectionComments(rejectionComments);
        travelRequest.setFirstLevelApprovalDate(LocalDate.now());

        return travelRequestRepository.save(travelRequest);
    }

    // Second-level approval
    @Override
    public TravelRequest approveSecondLevel(Long id, Long approverId, String approvalComments2) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));

        if (travelRequest.getFirstLevelApprovalStatus() != TravelStatus.APPROVED) {
            throw new IllegalStateException("First-level approval required before second-level approval.");
        }

        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        travelRequest.setSecondLevelApprovalStatus(TravelStatus.APPROVED);
        travelRequest.setSecondLevelApprover(approverEmployee.getUsername());
        travelRequest.setApprovalComments2(approvalComments2);
        travelRequest.setSecondLevelApprovalDate(LocalDate.now());
        return travelRequestRepository.save(travelRequest);
    }

    // Second-level rejection
    @Override
    public TravelRequest rejectSecondLevel(Long id, Long approverId, String rejectionComments) {
        TravelRequest travelRequest = travelRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));

        if (travelRequest.getFirstLevelApprovalStatus() != TravelStatus.APPROVED) {
            throw new IllegalStateException("First-level approval required before second-level rejection.");
        }

        Employee approverEmployee = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found."));

        travelRequest.setSecondLevelApprovalStatus(TravelStatus.REJECTED);
        travelRequest.setSecondLevelApprover(approverEmployee.getUsername());
        travelRequest.setRejectionComments(rejectionComments);
        travelRequest.setSecondLevelApprovalDate(LocalDate.now());

        return travelRequestRepository.save(travelRequest);
    }

    @Override
    public TravelRequest uploadDocuments(Long travelRequestId, List<MultipartFile> files) throws IOException {
        TravelRequest travelRequest = travelRequestRepository.findById(travelRequestId)
                .orElseThrow(() -> new RuntimeException("Travel Request not found"));

        Employee employee = travelRequest.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Travel Request must be associated with an employee");
        }
        String userId = employee.getId().toString();

        String documentUrl = s3Service.uploadCategoryDocuments(files, userId, "travelrequests");
        travelRequest.getDocumentUrls().add(documentUrl);

        return travelRequestRepository.save(travelRequest);
    }

    @Override
    public List<TravelRequest> getTravelRequestsByFirstLevelApprovalStatus(TravelStatus status) {
        return travelRequestRepository.findByFirstLevelApprovalStatus(status);
    }

    @Override
    public List<TravelRequest> getTravelRequestsByFirstLevelApprovalStatusAndEmployeeId(TravelStatus status, Long employeeId) {
        return travelRequestRepository.findByFirstLevelApprovalStatusAndEmployee_Id(status, employeeId);
    }

    // New methods for two-level approval
    @Override
    public List<TravelRequest> getFullyApprovedTravelRequests() {
        return travelRequestRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(TravelStatus.APPROVED, TravelStatus.APPROVED);
    }

    @Override
    public List<TravelRequest> getFullyApprovedTravelRequestsByEmployeeId(Long employeeId) {
        return travelRequestRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(TravelStatus.APPROVED, TravelStatus.APPROVED, employeeId);
    }

    @Override
    public List<TravelRequest> getPendingSecondLevelTravelRequests() {
        return travelRequestRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(TravelStatus.APPROVED, TravelStatus.PENDING);
    }

    @Override
    public List<TravelRequest> getPendingSecondLevelTravelRequestsByEmployeeId(Long employeeId) {
        return travelRequestRepository.findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(TravelStatus.APPROVED, TravelStatus.PENDING, employeeId);
    }
}
