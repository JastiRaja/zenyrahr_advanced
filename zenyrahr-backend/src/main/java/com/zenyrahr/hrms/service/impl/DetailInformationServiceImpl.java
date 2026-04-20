package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.DetailInformation;
import com.zenyrahr.hrms.Repository.DetailInformationRepository;
import com.zenyrahr.hrms.service.DetailInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetailInformationServiceImpl implements DetailInformationService {

    private final DetailInformationRepository detailInformationRepository;

    @Override
    public DetailInformation createDetailInformation(DetailInformation detailInformation) {
        return detailInformationRepository.save(detailInformation);
    }

    @Override
    public Optional<DetailInformation> getDetailInformationById(Long id) {
        return detailInformationRepository.findById(id);
    }

    @Override
    public List<DetailInformation> getAllDetailInformation() {
        return detailInformationRepository.findAll();
    }

    @Override
    public DetailInformation updateDetailInformation(Long id, DetailInformation updatedDetailInformation) {
        return detailInformationRepository.findById(id).map(existing -> {
            existing.setUsername(updatedDetailInformation.getUsername());
            existing.setDob(updatedDetailInformation.getDob());
            existing.setSurname(updatedDetailInformation.getSurname());
            existing.setFatherName(updatedDetailInformation.getFatherName());
            existing.setMotherName(updatedDetailInformation.getMotherName());
            existing.setImageUrl(updatedDetailInformation.getImageUrl());
            return detailInformationRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("DetailInformation not found with id: " + id));
    }

    @Override
    public void deleteDetailInformation(Long id) {
        detailInformationRepository.deleteById(id);
    }
}