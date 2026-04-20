package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Table(name = "TaxSlab")
@Entity
public class TaxSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // To differentiate between the old and new tax regimes
    @Column(nullable = false)
    private String regimeType; // "old" or "new"

    @Column(nullable = false)
    private BigDecimal incomeRangeStart;

    @Column(nullable = false)
    private BigDecimal incomeRangeEnd;

    @Column(nullable = false)
    private BigDecimal taxRate; // Tax rate for this range (e.g., 0.05 for 5%)

    private BigDecimal cessRate; // Optional, if different cess rates exist per regime
}