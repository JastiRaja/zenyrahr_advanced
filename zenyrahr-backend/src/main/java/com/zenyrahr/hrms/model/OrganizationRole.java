package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
