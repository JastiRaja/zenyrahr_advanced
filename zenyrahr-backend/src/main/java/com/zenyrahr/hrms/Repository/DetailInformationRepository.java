package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.DetailInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetailInformationRepository extends JpaRepository<DetailInformation, Long> {
    DetailInformation findByImageUrl(String imageUrl);
}