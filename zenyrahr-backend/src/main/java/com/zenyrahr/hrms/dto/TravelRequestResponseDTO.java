package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.TravelRequest;
import com.zenyrahr.hrms.model.TravelStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelRequestResponseDTO {
    private Long id;
    private String destination;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private Float budget;
    private String description;
    private String transportation;
    private String accommodation;
    private TravelStatus status;
    private TravelStatus firstLevelApprovalStatus;
    private TravelStatus secondLevelApprovalStatus;
    private String firstLevelApprover;
    private String secondLevelApprover;
    private String approvalComments1;
    private String approvalComments2;
    private LocalDate firstLevelApprovalDate;
    private LocalDate secondLevelApprovalDate;
    private String rejectionComments;
    private List<String> documentUrls;
    private EmployeeSummaryDTO employee;
    
    public static TravelRequestResponseDTO fromTravelRequest(TravelRequest travelRequest) {
        TravelRequestResponseDTO dto = new TravelRequestResponseDTO();
        dto.setId(travelRequest.getId());
        dto.setDestination(travelRequest.getDestination());
        dto.setPurpose(travelRequest.getPurpose());
        dto.setStartDate(travelRequest.getStartDate());
        dto.setEndDate(travelRequest.getEndDate());
        dto.setBudget(travelRequest.getBudget());
        dto.setDescription(travelRequest.getDescription());
        dto.setTransportation(travelRequest.getTransportation());
        dto.setAccommodation(travelRequest.getAccommodation());
        dto.setStatus(travelRequest.getStatus());
        dto.setFirstLevelApprovalStatus(travelRequest.getFirstLevelApprovalStatus());
        dto.setSecondLevelApprovalStatus(travelRequest.getSecondLevelApprovalStatus());
        dto.setFirstLevelApprover(travelRequest.getFirstLevelApprover());
        dto.setSecondLevelApprover(travelRequest.getSecondLevelApprover());
        dto.setApprovalComments1(travelRequest.getApprovalComments1());
        dto.setApprovalComments2(travelRequest.getApprovalComments2());
        dto.setFirstLevelApprovalDate(travelRequest.getFirstLevelApprovalDate());
        dto.setSecondLevelApprovalDate(travelRequest.getSecondLevelApprovalDate());
        dto.setRejectionComments(travelRequest.getRejectionComments());
        dto.setDocumentUrls(travelRequest.getDocumentUrls());
        
        if (travelRequest.getEmployee() != null) {
            EmployeeSummaryDTO employeeSummary = new EmployeeSummaryDTO();
            employeeSummary.setName(travelRequest.getEmployee().getFirstName() + " " + travelRequest.getEmployee().getLastName());
            employeeSummary.setRole(travelRequest.getEmployee().getRole());
            employeeSummary.setDepartment(travelRequest.getEmployee().getDepartment());
            dto.setEmployee(employeeSummary);
        }
        
        return dto;
    }
} 