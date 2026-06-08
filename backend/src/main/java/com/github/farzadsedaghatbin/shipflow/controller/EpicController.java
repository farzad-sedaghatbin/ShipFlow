package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.EpicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/epics")
@RequiredArgsConstructor
@Tag(name = "Epics", description = "Epic management")
public class EpicController {

  private final EpicService epicService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all epics across projects")
  public ResponseEntity<List<EpicDTO>> getAllEpics() {
    return ResponseEntity.ok(epicService.getAllEpics());
  }

  @GetMapping("/project/{projectId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all epics for a project")
  public ResponseEntity<List<EpicDTO>> getEpicsByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(epicService.getEpicsByProjectId(projectId));
  }

  @GetMapping("/project/{projectId}/orphans")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get epics not linked to any initiative")
  public ResponseEntity<List<EpicDTO>> getOrphanEpics(@PathVariable Long projectId) {
    return ResponseEntity.ok(epicService.getOrphanEpics(projectId));
  }

  @GetMapping("/initiative/{initiativeId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get epics by initiative ID")
  public ResponseEntity<List<EpicDTO>> getEpicsByInitiative(@PathVariable Long initiativeId) {
    return ResponseEntity.ok(epicService.getEpicsByInitiativeId(initiativeId));
  }

  @GetMapping("/project/{projectId}/status/{status}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get epics by project and status")
  public ResponseEntity<List<EpicDTO>> getEpicsByProjectAndStatus(
      @PathVariable Long projectId,
      @PathVariable EpicStatus status) {
    return ResponseEntity.ok(epicService.getEpicsByProjectIdAndStatus(projectId, status));
  }

  @GetMapping("/project/{projectId}/active")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get active epics for a project (PLANNED or IN_PROGRESS)")
  public ResponseEntity<List<EpicDTO>> getActiveEpics(@PathVariable Long projectId) {
    return ResponseEntity.ok(epicService.getActiveEpics(projectId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get epic by ID")
  public ResponseEntity<EpicDTO> getEpicById(@PathVariable Long id) {
    return ResponseEntity.ok(epicService.getEpicById(id));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.CREATE)
  @Operation(summary = "Create a new epic")
  public ResponseEntity<EpicDTO> createEpic(@Valid @RequestBody CreateEpicRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(epicService.createEpic(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Update an epic")
  public ResponseEntity<EpicDTO> updateEpic(
      @PathVariable Long id,
      @Valid @RequestBody CreateEpicRequest request) {
    return ResponseEntity.ok(epicService.updateEpic(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Update epic status")
  public ResponseEntity<EpicDTO> updateEpicStatus(
      @PathVariable Long id,
      @RequestParam EpicStatus status) {
    return ResponseEntity.ok(epicService.updateStatus(id, status));
  }

  @PatchMapping("/{id}/link-initiative")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Link epic to an initiative")
  public ResponseEntity<EpicDTO> linkToInitiative(
      @PathVariable Long id,
      @RequestParam Long initiativeId) {
    return ResponseEntity.ok(epicService.linkToInitiative(id, initiativeId));
  }

  @PatchMapping("/{id}/unlink-initiative")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Unlink epic from its initiative")
  public ResponseEntity<EpicDTO> unlinkFromInitiative(@PathVariable Long id) {
    return ResponseEntity.ok(epicService.unlinkFromInitiative(id));
  }

  @PatchMapping("/{id}/dates")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Update epic target dates")
  public ResponseEntity<EpicDTO> updateEpicDates(
      @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return ResponseEntity.ok(epicService.updateDates(id, startDate, endDate));
  }

  @PatchMapping("/reorder")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.UPDATE)
  @Operation(summary = "Reorder epics (batch update sort order)")
  public ResponseEntity<Void> reorderEpics(@Valid @RequestBody ReorderRequest request) {
    epicService.reorder(request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.EPIC, permission = PermissionType.DELETE)
  @Operation(summary = "Delete an epic (soft delete)")
  public ResponseEntity<Void> deleteEpic(@PathVariable Long id) {
    epicService.deleteEpic(id);
    return ResponseEntity.noContent().build();
  }
}
