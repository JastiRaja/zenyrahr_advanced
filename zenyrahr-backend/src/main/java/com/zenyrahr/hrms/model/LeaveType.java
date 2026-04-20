package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Annual Leave", "Sick Leave"
    private Integer defaultBalance; // Default days allocated for this leave type

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
