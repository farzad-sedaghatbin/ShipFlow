package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEvidenceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EvidenceDTO;
import com.github.farzadsedaghatbin.shipflow.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidences")
@RequiredArgsConstructor
@Tag(name = "Evidences", description = "Evidence/blockers management")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
    @Operation(summary = "Get all evidences")
    public ResponseEntity<List<EvidenceDTO>> getAllEvidences() {
        return ResponseEntity.ok(evidenceService.getAllEvidences());
    }

    @GetMapping("/pitch/{pitchId}")
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
    @Operation(summary = "Get evidences by pitch ID")
    public ResponseEntity<List<EvidenceDTO>> getEvidencesByPitchId(@PathVariable Long pitchId) {
        return ResponseEntity.ok(evidenceService.getEvidencesByPitchId(pitchId));
    }

    @GetMapping("/person/{personId}")
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
    @Operation(summary = "Get evidences by person ID")
    public ResponseEntity<List<EvidenceDTO>> getEvidencesByPersonId(@PathVariable Long personId) {
        return ResponseEntity.ok(evidenceService.getEvidencesByPersonId(personId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'READ')")
    @Operation(summary = "Get evidence by ID")
    public ResponseEntity<EvidenceDTO> getEvidenceById(@PathVariable Long id) {
        return ResponseEntity.ok(evidenceService.getEvidenceById(id));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
    @Operation(summary = "Create a new evidence")
    public ResponseEntity<EvidenceDTO> createEvidence(@Valid @RequestBody CreateEvidenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceService.createEvidence(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'UPDATE')")
    @Operation(summary = "Update an evidence")
    public ResponseEntity<EvidenceDTO> updateEvidence(@PathVariable Long id, @Valid @RequestBody CreateEvidenceRequest request) {
        return ResponseEntity.ok(evidenceService.updateEvidence(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('PITCH', 'DELETE')")
    @Operation(summary = "Delete an evidence")
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long id) {
        evidenceService.deleteEvidence(id);
        return ResponseEntity.noContent().build();
    }
}
