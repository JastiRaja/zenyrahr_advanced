package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.JobInformation;
import com.zenyrahr.hrms.service.JobInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-information")
@RequiredArgsConstructor
public class JobInformationController {

    private final JobInformationService jobInformationService;

    @GetMapping
    public ResponseEntity<List<JobInformation>> getAllJobInformation() {
        return ResponseEntity.ok(jobInformationService.getAllJobInformation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobInformation> getJobInformationById(@PathVariable Long id) {
        return jobInformationService.getJobInformationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<JobInformation> createJobInformation(@RequestBody JobInformation jobInformation) {
        return ResponseEntity.ok(jobInformationService.createJobInformation(jobInformation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobInformation> updateJobInformation(@PathVariable Long id, @RequestBody JobInformation jobInformation) {
        return ResponseEntity.ok(jobInformationService.updateJobInformation(id, jobInformation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobInformation(@PathVariable Long id) {
        jobInformationService.deleteJobInformation(id);
        return ResponseEntity.noContent().build();
    }
}
