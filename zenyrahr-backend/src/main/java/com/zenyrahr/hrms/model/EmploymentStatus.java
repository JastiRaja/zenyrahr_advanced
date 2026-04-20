package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "EmploymentStatus")
@Data
public class EmploymentStatus extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employment_status_seq")
    @SequenceGenerator(name = "employment_status_seq", sequenceName = "employment_status_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false)
    private String status;

//    @Column
//    private String description;
}
