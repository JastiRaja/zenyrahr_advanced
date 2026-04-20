package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.CountryDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryDetailsRepository extends JpaRepository<CountryDetails, Long> {
}
