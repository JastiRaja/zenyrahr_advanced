package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.model.StateDetails;
import com.zenyrahr.hrms.service.StateDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
public class StateDetailsController {

    private final StateDetailsService stateDetailsService;

    @PostMapping
    public ResponseEntity<StateDetails> createState(@RequestBody StateDetails stateDetails) {
        return ResponseEntity.ok(stateDetailsService.createState(stateDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateDetails> getStateById(@PathVariable Long id) {
        return stateDetailsService.getStateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<StateDetails>> getAllStates() {
        return ResponseEntity.ok(stateDetailsService.getAllStates());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StateDetails> updateState(@PathVariable Long id, @RequestBody StateDetails stateDetails) {
        return ResponseEntity.ok(stateDetailsService.updateState(id, stateDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteState(@PathVariable Long id) {
        stateDetailsService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}
