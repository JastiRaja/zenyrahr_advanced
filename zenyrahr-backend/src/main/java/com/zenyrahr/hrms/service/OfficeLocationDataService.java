package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.OfficeLocation;

import java.util.List;
import java.util.Optional;

public interface OfficeLocationDataService {

    OfficeLocation createOfficeLocation(OfficeLocation officeLocation);

    Optional<OfficeLocation> getOfficeLocationById(Long id);

    List<OfficeLocation> getAllOfficeLocations();

    OfficeLocation updateOfficeLocation(Long id, OfficeLocation officeLocation);

    void deleteOfficeLocation(Long id);
}
