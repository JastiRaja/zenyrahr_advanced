package com.zenyrahr.hrms.Repository;
import com.zenyrahr.hrms.model.DetailInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface S3Repository extends JpaRepository<DetailInformation, Long> {
    DetailInformation findByImageUrl(String imageUrl);
}