package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.TaxSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxSlabRepository extends JpaRepository<TaxSlab, Long> {
    List<TaxSlab> findByRegimeType(String regimeType);
}

