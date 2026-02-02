package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.AuditService;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pitches")
@RequiredArgsConstructor
@Tag(name = "Pitches", description = "Pitch management")
public class PitchController {

  private final PitchService pitchService;
  private final AuditService auditService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all pitches (Admin only)")
  public ResponseEntity<List<PitchDTO>> getAllPitches() {
    return ResponseEntity.ok(pitchService.getAllPitches());
  }

  @GetMapping("/my-pitches")
  @Operation(summary = "Get pitches accessible to current user")
  public ResponseEntity<List<PitchDTO>> getMyPitches() {
    return ResponseEntity.ok(pitchService.getAccessiblePitches());
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

  @GetMapping("/{id}/history")
  public ResponseEntity<Page<EntityHistoryDTO>> getPitchHistory(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(auditService.getPitchHistory(id, pageable));
  }

  @PostMapping
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.CREATE)
  @Operation(summary = "Create a new pitch")
  public ResponseEntity<PitchDTO> createPitch(@Valid @RequestBody CreatePitchRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(pitchService.createPitch(request));
  }

  @PutMapping("/{id}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Update a pitch")
  public ResponseEntity<PitchDTO> updatePitch(
      @PathVariable Long id, @Valid @RequestBody CreatePitchRequest request) {
    return ResponseEntity.ok(pitchService.updatePitch(id, request));
  }

  @PatchMapping("/{id}/status")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.APPROVE)
  @Operation(summary = "Update pitch status")
  public ResponseEntity<PitchDTO> updateStatus(
      @PathVariable Long id, @RequestParam PitchStatus status) {
    return ResponseEntity.ok(pitchService.updateStatus(id, status));
  }

  @PatchMapping("/{id}/assign-team/{teamId}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Assign team to pitch")
  public ResponseEntity<PitchDTO> assignTeam(@PathVariable Long id, @PathVariable Long teamId) {
    return ResponseEntity.ok(pitchService.assignTeam(id, teamId));
  }

  @DeleteMapping("/{id}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.DELETE)
  @Operation(summary = "Delete a pitch")
  public ResponseEntity<Void> deletePitch(@PathVariable Long id) {
    pitchService.deletePitch(id);
    return ResponseEntity.noContent().build();
  }
}
