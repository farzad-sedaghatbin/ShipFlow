package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pitches")
@RequiredArgsConstructor
@Tag(name = "Pitches", description = "Pitch management")
public class PitchController {

    private final PitchService pitchService;

    @GetMapping
    @Operation(summary = "Get all pitches")
    public ResponseEntity<List<PitchDTO>> getAllPitches() {
        return ResponseEntity.ok(pitchService.getAllPitches());
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get pitches by cycle ID")
    public ResponseEntity<List<PitchDTO>> getPitchesByCycleId(@PathVariable Long cycleId) {
        return ResponseEntity.ok(pitchService.getPitchesByCycleId(cycleId));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get pitches by team ID")
    public ResponseEntity<List<PitchDTO>> getPitchesByTeamId(@PathVariable Long teamId) {
        return ResponseEntity.ok(pitchService.getPitchesByTeamId(teamId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pitch by ID")
    public ResponseEntity<PitchDTO> getPitchById(@PathVariable Long id) {
        return ResponseEntity.ok(pitchService.getPitchById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new pitch")
    public ResponseEntity<PitchDTO> createPitch(@Valid @RequestBody CreatePitchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pitchService.createPitch(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pitch")
    public ResponseEntity<PitchDTO> updatePitch(@PathVariable Long id, @Valid @RequestBody CreatePitchRequest request) {
        return ResponseEntity.ok(pitchService.updatePitch(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update pitch status")
    public ResponseEntity<PitchDTO> updateStatus(@PathVariable Long id, @RequestParam PitchStatus status) {
        return ResponseEntity.ok(pitchService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign-team/{teamId}")
    @Operation(summary = "Assign team to pitch")
    public ResponseEntity<PitchDTO> assignTeam(@PathVariable Long id, @PathVariable Long teamId) {
        return ResponseEntity.ok(pitchService.assignTeam(id, teamId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pitch")
    public ResponseEntity<Void> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.noContent().build();
    }
}
