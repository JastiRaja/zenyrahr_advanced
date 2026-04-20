package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.Documents;
import com.zenyrahr.hrms.Repository.DocumentsRepository;
import com.zenyrahr.hrms.service.DocumentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentsServiceImpl implements DocumentsService {

    @Autowired
    private DocumentsRepository documentsRepository;

    @Override
    public Documents saveDocuments(Documents documents) {
        return documentsRepository.save(documents);
    }

    @Override
    public List<Documents> getAllDocuments() {
        return documentsRepository.findAll();
    }

    @Override
    public Optional<Documents> getDocumentsById(Long id) {
        return documentsRepository.findById(id);
    }

//    @Override
//    public Documents updateDocuments(Long id, Documents documents) {
//        Optional<Documents> existingDocuments = documentsRepository.findById(id);
//
//        if (existingDocuments.isPresent()) {
//            Documents updatedDocuments = existingDocuments.get();
//            updatedDocuments.setEducationDocumentsUrl(documents.getEducationDocumentsUrl());
//            updatedDocuments.setPersonalDocumentsUrl(documents.getPersonalDocumentsUrl());
//            updatedDocuments.setMedicalDocumentsUrl(documents.getMedicalDocumentsUrl());
//            updatedDocuments.setExperienceDocumentsUrl(documents.getExperienceDocumentsUrl());
//            updatedDocuments.setEmployee(documents.getEmployee());
//            return documentsRepository.save(updatedDocuments);
//        } else {
//            throw new RuntimeException("Document not found with id " + id);
//        }
//    }

    @Override
    public void deleteDocuments(Long id) {
        documentsRepository.deleteById(id);
    }

    @Override
    public Documents updateDocuments(Long id, Documents documents) {
        return documentsRepository.findById(id)
                .map(existing -> {
                    if (documents.getEducationDocumentsUrl() != null) {
                        existing.setEducationDocumentsUrl(documents.getEducationDocumentsUrl());
                    }
                    if (documents.getMedicalDocumentsUrl() != null) {
                        existing.setMedicalDocumentsUrl(documents.getMedicalDocumentsUrl());
                    }
                    if (documents.getExperienceDocumentsUrl() != null) {
                        existing.setExperienceDocumentsUrl(documents.getExperienceDocumentsUrl());
                    }
                    if (documents.getPersonalDocumentsUrl() != null) {
                        existing.setPersonalDocumentsUrl(documents.getPersonalDocumentsUrl());
                    }
                    if (documents.getProfileImageUrl() != null) {
                        existing.setProfileImageUrl(documents.getProfileImageUrl());
                    }
                    return documentsRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Document not found with id " + id));
    }

}
