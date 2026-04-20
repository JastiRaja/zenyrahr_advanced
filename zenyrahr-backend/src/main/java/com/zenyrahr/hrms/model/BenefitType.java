package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "BenefitType")
@Data
public class BenefitType extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefit_type_seq")
    @SequenceGenerator(name = "benefit_type_seq", sequenceName = "benefit_type_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;
}
