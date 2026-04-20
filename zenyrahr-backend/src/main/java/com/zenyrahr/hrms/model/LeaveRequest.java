package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class LeaveRequest extends BaseEntity2 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Employee employee;

    @ManyToOne
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;
    private String comments;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;
    private Boolean revocationRequested = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "leaveRequest_documents", joinColumns = @JoinColumn(name = "leaveRequest_id"))
    @Column(name = "document_url", length = 1000)
    private List<String> documentUrls = new ArrayList<>();
}
