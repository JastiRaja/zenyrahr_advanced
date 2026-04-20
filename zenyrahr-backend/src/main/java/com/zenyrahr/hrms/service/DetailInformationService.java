package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.DetailInformation;
import java.util.List;
import java.util.Optional;

public interface DetailInformationService {
    DetailInformation createDetailInformation(DetailInformation detailInformation);
    Optional<DetailInformation> getDetailInformationById(Long id);
    List<DetailInformation> getAllDetailInformation();
    DetailInformation updateDetailInformation(Long id, DetailInformation updatedDetailInformation);
    void deleteDetailInformation(Long id);
}