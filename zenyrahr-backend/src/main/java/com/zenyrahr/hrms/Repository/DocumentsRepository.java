package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Documents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentsRepository extends JpaRepository<Documents,Long> {
    List<Documents> findByEmployee_Id(Long employeeId);
}
