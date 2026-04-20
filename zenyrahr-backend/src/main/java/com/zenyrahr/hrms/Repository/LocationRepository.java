package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findAllByDeletedFalse();
    List<Location> findByOrganization_Id(Long organizationId);
}
