package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.CountryDetails;

import java.util.List;
import java.util.Optional;

public interface CountryDetailsService {

    CountryDetails createCountryDetails(CountryDetails countryDetails);

    Optional<CountryDetails> getCountryDetailsById(Long id);

    List<CountryDetails> getAllCountryDetails();

    CountryDetails updateCountryDetails(Long id, CountryDetails countryDetails);

    void deleteCountryDetails(Long id);
}
