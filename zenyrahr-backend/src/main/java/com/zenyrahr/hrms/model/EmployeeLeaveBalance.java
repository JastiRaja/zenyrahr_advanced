package com.zenyrahr.hrms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class EmployeeLeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference("leaveBalance-reference")
    private Employee employee;

    @ManyToOne
    @JsonBackReference
    private LeaveType leaveType;

    private Integer balance; // Remaining days for this leave type
}
