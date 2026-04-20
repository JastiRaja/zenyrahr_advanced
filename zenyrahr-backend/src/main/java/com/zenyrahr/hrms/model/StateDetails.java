package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "StateDetails")
@Data
public class StateDetails extends BaseEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "state_seq")
    @SequenceGenerator(name = "state_seq", sequenceName = "state_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryDetails countryId;
}
