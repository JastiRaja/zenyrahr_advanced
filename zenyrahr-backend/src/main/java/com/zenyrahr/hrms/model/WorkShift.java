package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Entity
@Table(name = "WorkShift")
public class WorkShift extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_shift_seq")
    @SequenceGenerator(name = "work_shift_seq", sequenceName = "work_shift_seq", allocationSize = 5)
    private Long id;
//
//    @Column(nullable = false)
//    private String name;

    @Column
    private String startTime;

    @Column
    private String endTime;
//
//    @Column
//    private String description;

    public WorkShift() {
        super();
    }
}
