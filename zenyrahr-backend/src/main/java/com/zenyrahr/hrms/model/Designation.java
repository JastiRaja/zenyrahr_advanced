package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "Designation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"})
)
@Data
public class Designation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "designation_seq")
    @SequenceGenerator(name = "designation_seq", sequenceName = "designation_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
