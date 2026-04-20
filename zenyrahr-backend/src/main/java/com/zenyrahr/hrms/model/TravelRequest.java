package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "travel")
public class TravelRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @JsonIgnore
    private Long id;

        private String destination;

        private  String purpose;

        private LocalDate startDate;

        private LocalDate endDate;

        private Float budget;

        private String description;

        private String transportation;

        private String accommodation;

        @Enumerated(EnumType.STRING)
        private  TravelStatus status;

        @Enumerated(EnumType.STRING)
        private TravelStatus firstLevelApprovalStatus;

        @Enumerated(EnumType.STRING)
        private TravelStatus secondLevelApprovalStatus;

        private String firstLevelApprover;

        private String secondLevelApprover;

        private String approvalComments1;

        private String approvalComments2;

        private LocalDate firstLevelApprovalDate;

        private LocalDate secondLevelApprovalDate;

        @Column(length = 500) // Optional: limit the size of the comment
        private String rejectionComments;

        @ElementCollection
        @CollectionTable(name = "travel_request_documents", joinColumns = @JoinColumn(name = "travel_request_id"))
        @Column(name = "document_url", length = 1000)
        private List<String> documentUrls = new ArrayList<>();

        @ManyToOne
        @JoinColumn(name = "employee_id")
        private Employee employee;

}
