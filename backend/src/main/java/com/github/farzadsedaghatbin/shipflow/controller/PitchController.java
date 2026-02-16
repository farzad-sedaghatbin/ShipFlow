package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateIdeaRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.AuditService;
import com.github.farzadsedaghatbin.shipflow.service.PitchDocumentEnhancementService;
import com.github.farzadsedaghatbin.shipflow.service.PitchDocumentEnhancementService.EnhancementOptions;
import com.github.farzadsedaghatbin.shipflow.service.PitchDocumentEnhancementService.EnhancementResult;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
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
  private final PitchDocumentEnhancementService enhancementService;

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
  public ResponseEntity<Page<EntityHistoryDTO>> getPitchHistory(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
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
  public ResponseEntity<PitchDTO> updatePitch(@PathVariable Long id, @Valid @RequestBody CreatePitchRequest request) {
    return ResponseEntity.ok(pitchService.updatePitch(id, request));
  }

  @PatchMapping("/{id}/status")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.APPROVE)
  @Operation(summary = "Update pitch status")
  public ResponseEntity<PitchDTO> updateStatus(@PathVariable Long id, @RequestParam PitchStatus status) {
    return ResponseEntity.ok(pitchService.updateStatus(id, status));
  }

  @PatchMapping("/{id}/assign-team/{teamId}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Assign team to pitch")
  public ResponseEntity<PitchDTO> assignTeam(@PathVariable Long id, @PathVariable Long teamId) {
    return ResponseEntity.ok(pitchService.assignTeam(id, teamId));
  }

  @GetMapping("/epic/{epicId}")
  @Operation(summary = "Get pitches by epic ID")
  public ResponseEntity<List<PitchDTO>> getPitchesByEpicId(@PathVariable Long epicId) {
    return ResponseEntity.ok(pitchService.getPitchesByEpicId(epicId));
  }

  @GetMapping("/release/{releaseId}")
  @Operation(summary = "Get pitches by release ID")
  public ResponseEntity<List<PitchDTO>> getPitchesByReleaseId(@PathVariable Long releaseId) {
    return ResponseEntity.ok(pitchService.getPitchesByReleaseId(releaseId));
  }

  @PatchMapping("/{id}/epic/{epicId}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Link pitch to epic")
  public ResponseEntity<PitchDTO> linkToEpic(@PathVariable Long id, @PathVariable Long epicId) {
    return ResponseEntity.ok(pitchService.linkToEpic(id, epicId));
  }

  @PatchMapping("/{id}/unlink-epic")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Unlink pitch from epic")
  public ResponseEntity<PitchDTO> unlinkFromEpic(@PathVariable Long id) {
    return ResponseEntity.ok(pitchService.unlinkFromEpic(id));
  }

  @PatchMapping("/{id}/release/{releaseId}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Set target release for pitch")
  public ResponseEntity<PitchDTO> setTargetRelease(@PathVariable Long id, @PathVariable Long releaseId) {
    return ResponseEntity.ok(pitchService.setTargetRelease(id, releaseId));
  }

  @PatchMapping("/{id}/unlink-release")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Clear target release from pitch")
  public ResponseEntity<PitchDTO> clearTargetRelease(@PathVariable Long id) {
    return ResponseEntity.ok(pitchService.clearTargetRelease(id));
  }

  // ===== Shape Up Workflow: Ideas Pool =====

  @GetMapping("/ideas")
  @Operation(summary = "Get all ideas (raw concepts not yet being shaped)")
  public ResponseEntity<List<PitchDTO>> getIdeas() {
    return ResponseEntity.ok(pitchService.getIdeas());
  }

  @GetMapping("/project/{projectId}/ideas")
  @Operation(summary = "Get ideas by project ID")
  public ResponseEntity<List<PitchDTO>> getIdeasByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(pitchService.getIdeasByProjectId(projectId));
  }

  @GetMapping("/epic/{epicId}/ideas")
  @Operation(summary = "Get ideas by epic ID")
  public ResponseEntity<List<PitchDTO>> getIdeasByEpicId(@PathVariable Long epicId) {
    return ResponseEntity.ok(pitchService.getIdeasByEpicId(epicId));
  }

  @PostMapping("/ideas")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.CREATE)
  @Operation(summary = "Create a lightweight idea (just title + optional description)")
  public ResponseEntity<PitchDTO> createIdea(@Valid @RequestBody CreateIdeaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(pitchService.createIdea(request.getTitle(), request.getDescription(), request.getEpicId()));
  }

  // ===== Shape Up Workflow: Shaping Pool =====

  @GetMapping("/drafts")
  @Operation(summary = "Get all drafts (pitches being shaped)")
  public ResponseEntity<List<PitchDTO>> getDrafts() {
    return ResponseEntity.ok(pitchService.getDrafts());
  }

  @GetMapping("/project/{projectId}/drafts")
  @Operation(summary = "Get drafts by project ID")
  public ResponseEntity<List<PitchDTO>> getDraftsByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(pitchService.getDraftsByProjectId(projectId));
  }

  // ===== Shape Up Workflow: Betting Table =====

  @GetMapping("/betting-candidates")
  @Operation(summary = "Get shaped pitches ready for betting (cycle assignment)")
  public ResponseEntity<List<PitchDTO>> getBettingCandidates() {
    return ResponseEntity.ok(pitchService.getBettingCandidates());
  }

  @GetMapping("/project/{projectId}/betting-candidates")
  @Operation(summary = "Get betting candidates by project ID")
  public ResponseEntity<List<PitchDTO>> getBettingCandidatesByProjectId(@PathVariable Long projectId) {
    return ResponseEntity.ok(pitchService.getBettingCandidatesByProjectId(projectId));
  }

  @GetMapping("/epic/{epicId}/unassigned")
  @Operation(summary = "Get unassigned pitches (IDEA, DRAFT, SHAPED) by epic ID")
  public ResponseEntity<List<PitchDTO>> getUnassignedByEpicId(@PathVariable Long epicId) {
    return ResponseEntity.ok(pitchService.getUnassignedByEpicId(epicId));
  }

  // ===== Shape Up Workflow: Status Transitions =====

  @PatchMapping("/{id}/start-shaping")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Start shaping an idea (IDEA → DRAFT)")
  public ResponseEntity<PitchDTO> startShaping(@PathVariable Long id) {
    return ResponseEntity.ok(pitchService.startShaping(id));
  }

  @PatchMapping("/{id}/mark-shaped")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.APPROVE)
  @Operation(summary = "Mark draft as shaped and ready for betting (DRAFT → SHAPED)")
  public ResponseEntity<PitchDTO> markAsShaped(@PathVariable Long id) {
    return ResponseEntity.ok(pitchService.markAsShaped(id));
  }

  @PatchMapping("/{id}/assign-cycle/{cycleId}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.APPROVE)
  @Operation(summary = "Assign shaped pitch to cycle (betting decision) (SHAPED → PENDING)")
  public ResponseEntity<PitchDTO> assignToCycle(@PathVariable Long id, @PathVariable Long cycleId) {
    return ResponseEntity.ok(pitchService.assignToCycle(id, cycleId));
  }

  @PatchMapping("/{id}/unassign-cycle")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.APPROVE)
  @Operation(summary = "Unassign pitch from cycle (PENDING → SHAPED)")
  public ResponseEntity<PitchDTO> unassignFromCycle(@PathVariable Long id) {
    return ResponseEntity.ok(pitchService.unassignFromCycle(id));
  }

  @DeleteMapping("/{id}")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.DELETE)
  @Operation(summary = "Delete a pitch")
  public ResponseEntity<Void> deletePitch(@PathVariable Long id) {
    pitchService.deletePitch(id);
    return ResponseEntity.noContent().build();
  }

  // ===== AI Document Enhancement =====

  @PostMapping("/{id}/enhance-from-documents")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Enhance pitch content by extracting data from associated documents using AI")
  public ResponseEntity<EnhancementResult> enhanceFromDocuments(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean overwriteExisting,
      @RequestParam(defaultValue = "false") boolean autoAdvanceStatus) {
    EnhancementOptions options = new EnhancementOptions(overwriteExisting, autoAdvanceStatus);
    EnhancementResult result = enhancementService.enhancePitchFromDocuments(id, options);
    return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
  }

  @PostMapping("/{id}/enhance-from-text")
  @RequirePermission(resource = ResourceType.PITCH, permission = PermissionType.UPDATE)
  @Operation(summary = "Enhance pitch content by extracting data from provided text using AI")
  public ResponseEntity<EnhancementResult> enhanceFromText(
      @PathVariable Long id,
      @RequestBody String documentText,
      @RequestParam(defaultValue = "false") boolean overwriteExisting,
      @RequestParam(defaultValue = "false") boolean autoAdvanceStatus) {
    EnhancementOptions options = new EnhancementOptions(overwriteExisting, autoAdvanceStatus);
    EnhancementResult result = enhancementService.enhancePitchFromText(id, documentText, options);
    return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
  }

  @GetMapping("/enhancement-available")
  @Operation(summary = "Check if AI document enhancement is available")
  public ResponseEntity<Boolean> isEnhancementAvailable() {
    return ResponseEntity.ok(enhancementService.isEnhancementAvailable());
  }
}
