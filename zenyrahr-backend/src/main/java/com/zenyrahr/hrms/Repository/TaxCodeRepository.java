package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.TaxCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxCodeRepository extends JpaRepository<TaxCode, Long> {
}
