package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Documents;


import java.util.List;
import java.util.Optional;


public interface DocumentsService {
    Documents saveDocuments(Documents documents);
    List<Documents>getAllDocuments();
    Optional<Documents> getDocumentsById(Long id);
    Documents updateDocuments(Long id,Documents documents);
    void deleteDocuments(Long id);
    
}
