package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "organization_role",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"})
)
public class OrganizationRole {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "org_role_seq")
    @SequenceGenerator(name = "org_role_seq", sequenceName = "org_role_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "base_system_role", nullable = false, length = 32)
    private String baseSystemRole = "employee";

    @ElementCollection
    @CollectionTable(
            name = "organization_role_capability_pack",
            joinColumns = @JoinColumn(name = "organization_role_id")
    )
    @Column(name = "capability_pack", nullable = false, length = 40)
    private List<String> capabilityPacks = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
