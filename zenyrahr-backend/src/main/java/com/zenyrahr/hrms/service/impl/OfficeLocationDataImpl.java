package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.OfficeLocation;
import com.zenyrahr.hrms.Repository.OfficeLocationRepository;
import com.zenyrahr.hrms.service.OfficeLocationDataService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfficeLocationDataImpl implements OfficeLocationDataService {

    private final OfficeLocationRepository officeLocationRepository;
    private final SequenceService sequenceService;

    @Override
    public OfficeLocation createOfficeLocation(OfficeLocation officeLocation) {
        officeLocation.setCode(sequenceService.getNextCode("OFFLOC"));
        return officeLocationRepository.save(officeLocation);
    }

    @Override
    public Optional<OfficeLocation> getOfficeLocationById(Long id) {
        return officeLocationRepository.findById(id);
    }

    @Override
    public List<OfficeLocation> getAllOfficeLocations() {
        return officeLocationRepository.findAll();
    }

    @Override
    public OfficeLocation updateOfficeLocation(Long id, OfficeLocation officeLocation) {
        if (officeLocationRepository.existsById(id)) {
            officeLocation.setId(id);
            return officeLocationRepository.save(officeLocation);
        } else {
            throw new RuntimeException("OfficeLocation not found");
        }
    }

    @Override
    public void deleteOfficeLocation(Long id) {
        officeLocationRepository.deleteById(id);
    }
}
