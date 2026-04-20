package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.CountryDetails;
import com.zenyrahr.hrms.Repository.CountryDetailsRepository;
import com.zenyrahr.hrms.service.CountryDetailsService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountryDetailsServiceImpl implements CountryDetailsService {

    private final CountryDetailsRepository countryDetailsRepository;
    private final SequenceService sequenceService;


    @Override
    public CountryDetails createCountryDetails(CountryDetails countryDetails) {
        countryDetails.setCode(sequenceService.getNextCode("COUNTRY")); // Generating unique code
        return countryDetailsRepository.save(countryDetails); // Save the entity to the database
    }


    @Override
    public Optional<CountryDetails> getCountryDetailsById(Long id) {
        return countryDetailsRepository.findById(id);
    }

    @Override
    public List<CountryDetails> getAllCountryDetails() {
        return countryDetailsRepository.findAll();
    }

    @Override
    public CountryDetails updateCountryDetails(Long id, CountryDetails countryDetails) {
        if (countryDetailsRepository.existsById(id)) {
            countryDetails.setId(id); // Maintain existing ID
            return countryDetailsRepository.save(countryDetails);
        } else {
            throw new RuntimeException("Country Details not found");
        }
    }

    @Override
    public void deleteCountryDetails(Long id) {
        countryDetailsRepository.deleteById(id);
    }
}
