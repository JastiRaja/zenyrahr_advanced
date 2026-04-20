package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "TaxCode")
@Data
public class TaxCode extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tax_code_seq")
    @SequenceGenerator(name = "tax_code_seq", sequenceName = "tax_code_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false)
    private String code;

//    @Column
//    private String description;

    @Column(nullable = false)
    private Double rate;
}
