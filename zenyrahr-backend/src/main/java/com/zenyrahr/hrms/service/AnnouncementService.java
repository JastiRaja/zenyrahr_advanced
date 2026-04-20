package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.AnnouncementRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.dto.AnnouncementRequestDTO;
import com.zenyrahr.hrms.dto.AnnouncementResponseDTO;
import com.zenyrahr.hrms.model.Announcement;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;
    private final SequenceService sequenceService;

    public List<AnnouncementResponseDTO> getCurrentAnnouncements() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        if (tenantAccessService.isMainAdmin(actor)) {
            return List.of();
        }
        Long organizationId = tenantAccessService.requireOrganizationId(actor);
        return announcementRepository
                .findByOrganization_IdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AnnouncementResponseDTO> getAnnouncementHistory() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanPublishAnnouncements(actor);
        Long organizationId = tenantAccessService.requireOrganizationId(actor);
        return announcementRepository.findByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnnouncementResponseDTO createAnnouncement(AnnouncementRequestDTO payload) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanPublishAnnouncements(actor);

        String title = payload.getTitle() == null ? "" : payload.getTitle().trim();
        String message = payload.getMessage() == null ? "" : payload.getMessage().trim();

        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement title is required");
        }
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement message is required");
        }
        if (title.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement title must be 160 characters or less");
        }
        if (message.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement message must be 4000 characters or less");
        }

        Long organizationId = tenantAccessService.requireOrganizationId(actor);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found"));

        Announcement announcement = new Announcement();
        announcement.setCode(sequenceService.getNextCode("ANN"));
        announcement.setTitle(title);
        announcement.setMessage(message);
        announcement.setOrganization(organization);
        announcement.setPostedByName(buildDisplayName(actor));
        announcement.setPostedByRole(actor.getRole() == null ? "" : actor.getRole().toUpperCase(Locale.ROOT));
        announcement.setActive(true);
        announcement.setDeleted(false);

        return toResponse(announcementRepository.save(announcement));
    }

    @Transactional
    public void archiveAnnouncement(Long announcementId) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        assertCanPublishAnnouncements(actor);
        Long organizationId = tenantAccessService.requireOrganizationId(actor);

        Announcement announcement = announcementRepository.findByIdAndOrganization_Id(announcementId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        announcement.setActive(false);
        announcement.setDeleted(true);
        announcementRepository.save(announcement);
    }

    private void assertCanPublishAnnouncements(Employee actor) {
        if (!tenantAccessService.isOrgAdmin(actor) && !tenantAccessService.isHr(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization admin or HR can manage announcements");
        }
    }

    private String buildDisplayName(Employee employee) {
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        String full = (firstName + " " + lastName).trim();
        return full.isEmpty() ? "Unknown User" : full;
    }

    private AnnouncementResponseDTO toResponse(Announcement announcement) {
        return AnnouncementResponseDTO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .message(announcement.getMessage())
                .postedByName(announcement.getPostedByName())
                .postedByRole(announcement.getPostedByRole())
                .active(announcement.getActive())
                .deleted(announcement.getDeleted())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
