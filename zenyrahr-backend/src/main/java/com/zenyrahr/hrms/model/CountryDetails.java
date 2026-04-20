package com.zenyrahr.hrms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "CountryDetails")
@Data
public class CountryDetails extends BaseEntity2{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "country_details_seq")
    @SequenceGenerator(name = "country_details_seq", sequenceName = "country_details_seq", allocationSize = 5)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String countryName;

//    @OneToMany(mappedBy = "countryId")
//    @JsonBackReference
//    private List<StateDetails> stateDetails;
}
