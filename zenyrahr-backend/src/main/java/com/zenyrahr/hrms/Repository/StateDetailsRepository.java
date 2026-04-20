package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.StateDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateDetailsRepository extends JpaRepository<StateDetails, Long> {
}
