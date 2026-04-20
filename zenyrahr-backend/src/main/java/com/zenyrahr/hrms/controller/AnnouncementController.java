package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.dto.AnnouncementRequestDTO;
import com.zenyrahr.hrms.dto.AnnouncementResponseDTO;
import com.zenyrahr.hrms.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/current")
    public ResponseEntity<List<AnnouncementResponseDTO>> getCurrentAnnouncements() {
        return ResponseEntity.ok(announcementService.getCurrentAnnouncements());
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnnouncementResponseDTO>> getHistoryAnnouncements() {
        return ResponseEntity.ok(announcementService.getAnnouncementHistory());
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponseDTO> createAnnouncement(@RequestBody AnnouncementRequestDTO payload) {
        return ResponseEntity.ok(announcementService.createAnnouncement(payload));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveAnnouncement(@PathVariable Long id) {
        announcementService.archiveAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
}
