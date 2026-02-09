package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.service.RetroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/retros")
@RequiredArgsConstructor
@Tag(name = "Retrospectives", description = "Retrospective management for Shape Up cycles")
public class RetroController {

  private final RetroService retroService;

  // ==================== RETRO ENDPOINTS ====================

  @GetMapping("/project/{projectId}")
  @Operation(summary = "Get all retrospectives by project")
  public ResponseEntity<List<RetroDTO>> getRetrosByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(retroService.getAllRetrosByProject(projectId));
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get retrospectives by cycle")
  public ResponseEntity<List<RetroDTO>> getRetrosByCycle(@PathVariable Long cycleId) {
    return ResponseEntity.ok(retroService.getRetrosByCycle(cycleId));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get retrospective by ID")
  public ResponseEntity<RetroDTO> getRetroById(@PathVariable Long id) {
    return ResponseEntity.ok(retroService.getRetroById(id));
  }

  @GetMapping("/{id}/with-items")
  @Operation(summary = "Get retrospective with all items")
  public ResponseEntity<RetroDTO> getRetroWithItems(@PathVariable Long id) {
    return ResponseEntity.ok(retroService.getRetroWithItems(id));
  }

  @PostMapping
  @Operation(summary = "Create a new retrospective")
  public ResponseEntity<RetroDTO> createRetro(@Valid @RequestBody CreateRetroRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(retroService.createRetro(request));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a retrospective")
  public ResponseEntity<RetroDTO> updateRetro(@PathVariable Long id, @Valid @RequestBody UpdateRetroRequest request) {
    return ResponseEntity.ok(retroService.updateRetro(id, request));
  }

  @PostMapping("/{id}/open")
  @Operation(summary = "Open a retrospective for team participation")
  public ResponseEntity<RetroDTO> openRetro(@PathVariable Long id) {
    return ResponseEntity.ok(retroService.openRetro(id));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
  @Operation(summary = "Close a retrospective (Admin/Project Manager only)")
  public ResponseEntity<RetroDTO> closeRetro(@PathVariable Long id) {
    return ResponseEntity.ok(retroService.closeRetro(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete a retrospective (Admin only)")
  public ResponseEntity<Void> deleteRetro(@PathVariable Long id) {
    retroService.deleteRetro(id);
    return ResponseEntity.noContent().build();
  }

  // ==================== RETRO ITEM ENDPOINTS ====================

  @GetMapping("/{retroId}/items")
  @Operation(summary = "Get all items in a retrospective")
  public ResponseEntity<List<RetroItemDTO>> getRetroItems(@PathVariable Long retroId) {
    return ResponseEntity.ok(retroService.getItemsByRetro(retroId));
  }

  @PostMapping("/items")
  @Operation(summary = "Add an item to a retrospective")
  public ResponseEntity<RetroItemDTO> createRetroItem(@Valid @RequestBody CreateRetroItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(retroService.createRetroItem(request));
  }

  @PutMapping("/items/{itemId}")
  @Operation(summary = "Update a retrospective item")
  public ResponseEntity<RetroItemDTO> updateRetroItem(@PathVariable Long itemId,
      @RequestBody Map<String, String> body) {
    String content = body.get("content");
    return ResponseEntity.ok(retroService.updateRetroItem(itemId, content));
  }

  @DeleteMapping("/items/{itemId}")
  @Operation(summary = "Delete a retrospective item")
  public ResponseEntity<Void> deleteRetroItem(@PathVariable Long itemId) {
    retroService.deleteRetroItem(itemId);
    return ResponseEntity.noContent().build();
  }

  // ==================== VOTING ====================

  @PostMapping("/items/{itemId}/vote")
  @Operation(summary = "Toggle vote on a retrospective item (vote or unvote)")
  public ResponseEntity<RetroItemDTO> toggleVote(@PathVariable Long itemId) {
    return ResponseEntity.ok(retroService.toggleVote(itemId));
  }

  // ==================== MERGING ====================

  @PostMapping("/items/{targetItemId}/merge/{sourceItemId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
  @Operation(summary = "Merge one item into another (Admin/Project Manager only)")
  public ResponseEntity<RetroItemDTO> mergeItems(@PathVariable Long targetItemId, @PathVariable Long sourceItemId) {
    return ResponseEntity.ok(retroService.mergeItems(targetItemId, sourceItemId));
  }

  @PostMapping("/items/{itemId}/unmerge")
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
  @Operation(summary = "Unmerge an item (Admin/Project Manager only)")
  public ResponseEntity<RetroItemDTO> unmergeItem(@PathVariable Long itemId) {
    return ResponseEntity.ok(retroService.unmergeItem(itemId));
  }

  // ==================== CYCLE RETRO STATUS ====================

  @GetMapping("/cycle/{cycleId}/status")
  @Operation(summary = "Get cycle retrospective completion status")
  public ResponseEntity<CycleRetroStatusDTO> getCycleRetroStatus(@PathVariable Long cycleId) {
    return ResponseEntity.ok(retroService.getCycleRetroStatus(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/can-close")
  @Operation(summary = "Check if cycle can be closed (has at least one closed retro)")
  public ResponseEntity<Map<String, Boolean>> canCloseCycle(@PathVariable Long cycleId) {
    boolean canClose = retroService.canCloseCycle(cycleId);
    return ResponseEntity.ok(Map.of("canClose", canClose));
  }

  // ==================== PROJECT SETTING ====================

  @GetMapping("/project/{projectId}/enabled")
  @Operation(summary = "Check if retrospectives are enabled for a project")
  public ResponseEntity<Map<String, Boolean>> isRetrospectivesEnabled(@PathVariable Long projectId) {
    boolean enabled = retroService.isRetrospectivesEnabled(projectId);
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }

  @PutMapping("/project/{projectId}/enabled")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Enable or disable retrospectives for a project (Admin only)")
  public ResponseEntity<Map<String, Boolean>> setRetrospectivesEnabled(@PathVariable Long projectId,
      @RequestBody Map<String, Boolean> body) {
    Boolean enabled = body.get("enabled");
    if (enabled == null) {
      return ResponseEntity.badRequest().build();
    }
    retroService.setRetrospectivesEnabled(projectId, enabled);
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }

  // ==================== ACTION TRACKING (v0.5) ====================

  @PostMapping("/items/{itemId}/acted-on")
  @PreAuthorize("hasPermission(#itemId, 'RetroItem', 'UPDATE')")
  @Operation(summary = "Mark a retro item as acted upon", 
      description = "Part of v0.5 - tracks whether teams follow through on retro decisions")
  public ResponseEntity<RetroItemDTO> markActedOn(
      @PathVariable Long itemId,
      @Valid @RequestBody MarkActedOnRequest request) {
    return ResponseEntity.ok(retroService.markActedOn(itemId, request.getActedOn(), request.getNotes()));
  }

  @GetMapping("/{retroId}/action-stats")
  @PreAuthorize("hasPermission(#retroId, 'Retrospective', 'READ')")
  @Operation(summary = "Get action tracking statistics for a retrospective")
  public ResponseEntity<RetroActionStatsDTO> getActionStats(@PathVariable Long retroId) {
    return ResponseEntity.ok(retroService.getActionStats(retroId));
  }

  @GetMapping("/project/{projectId}/pending-actions")
  @PreAuthorize("hasPermission(#projectId, 'Project', 'READ')")
  @Operation(summary = "Get pending action items across all retros in a project")
  public ResponseEntity<List<RetroItemDTO>> getPendingActionItems(@PathVariable Long projectId) {
    return ResponseEntity.ok(retroService.getPendingActionItems(projectId));
  }

  // ==================== RETRO → PITCH CONVERSION (v0.5) ====================

  @PostMapping("/{id}/convert-to-pitch")
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'MANAGER')")
  @Operation(summary = "Convert closed retro action items into a pitch draft",
      description = "Part of v0.5 - enables closed retro insights to feed into next cycle's shaping")
  public ResponseEntity<PitchDTO> convertToPitchDraft(
      @PathVariable Long id,
      @Valid @RequestBody ConvertRetroToPitchRequest request) {
    // Override the request's retrospectiveId with the path parameter for security
    request.setRetrospectiveId(id);
    return ResponseEntity.status(HttpStatus.CREATED).body(retroService.convertToPitchDraft(request));
  }
}
