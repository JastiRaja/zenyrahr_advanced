package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "approval_hierarchy_rule",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_approval_rule_org_module_level", columnNames = {"organization_id", "module", "level_no"})
        }
)
@Data
public class ApprovalHierarchyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 32)
    private ApprovalModule module;

    @Column(name = "level_no", nullable = false)
    private Integer levelNo;

    @Column(name = "requester_role", length = 64)
    private String requesterRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 32)
    private ApproverType approverType;

    @Column(name = "approver_role", length = 64)
    private String approverRole;

    @ManyToOne
    @JoinColumn(name = "approver_user_id")
    private Employee approverUser;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}

