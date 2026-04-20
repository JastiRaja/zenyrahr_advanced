package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByOrganization_IdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(Long organizationId);
    List<Announcement> findByOrganization_IdOrderByCreatedAtDesc(Long organizationId);
    Optional<Announcement> findByIdAndOrganization_Id(Long id, Long organizationId);
}
