package com.zenyrahr.hrms.Repository;


import com.zenyrahr.hrms.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
}