package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private LocalDate date;

    private String category;

    private Float amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus firstLevelApprovalStatus;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus secondLevelApprovalStatus;

    private String firstLevelApprover;

    private String secondLevelApprover;

    private String approvalComments1;

    private String approvalComments2;

    private LocalDate firstLevelApprovalDate;

    private LocalDate secondLevelApprovalDate;

    @Column(length = 500) // Optional: limit the size of the comment
    private String rejectionComments;

    @ElementCollection
    @CollectionTable(name = "expense_documents", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "document_url", length = 1000)
    private List<String> documentUrls = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
