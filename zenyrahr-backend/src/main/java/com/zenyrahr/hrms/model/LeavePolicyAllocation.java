package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "leave_policy_allocation")
@Data
public class LeavePolicyAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private LeavePolicySetting policy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer days;
}
