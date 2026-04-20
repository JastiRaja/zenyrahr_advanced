package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Location;

import java.util.List;
import java.util.Optional;

public interface LocationService {

    Location createLocation(Location location);

    Optional<Location> getLocationById(Long id);

    List<Location> getAllLocation();

    Location updateLocation(Long id, Location locationDetails);

    void deleteLocation(Long id);

}
