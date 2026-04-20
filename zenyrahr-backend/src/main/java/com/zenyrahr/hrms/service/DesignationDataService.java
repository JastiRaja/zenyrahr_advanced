package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Designation;

import java.util.List;
import java.util.Optional;

public interface DesignationDataService {

    Designation createDesignation(Designation designation);

    Optional<Designation> getDesignationById(Long id);

    List<Designation> getAllDesignations();

    Designation updateDesignation(Long id, Designation designation);

    void deleteDesignation(Long id);
}
