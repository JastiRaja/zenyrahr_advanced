package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "EmploymentType")
@Data
public class EmploymentType extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employment_type_seq")
    @SequenceGenerator(name = "employment_type_seq", sequenceName = "employment_type_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false)
    private String type;

//    @Column
//    private String description;
}
