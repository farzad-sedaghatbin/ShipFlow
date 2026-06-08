package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.snapshot.ProjectSnapshotDTO;
import com.github.farzadsedaghatbin.shipflow.service.ProjectSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for project AI snapshots.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code GET /api/ai/projects/{projectId}/snapshot} — retrieve cached snapshot
 *   <li>{@code POST /api/ai/projects/{projectId}/snapshot/refresh} — force re-computation
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/projects")
@RequiredArgsConstructor
@Tag(name = "Project Snapshot", description = "Pre-computed project context snapshots for AI features")
public class ProjectSnapshotController {

  private final ProjectSnapshotService projectSnapshotService;

  /**
   * Returns the cached project snapshot (or computes one on first access).
   *
   * <p>Allowed roles: any principal with READ access on AI_FEATURES.
   */
  @GetMapping("/{projectId}/snapshot")
  @Operation(
      summary = "Get project AI snapshot",
      description =
          "Returns a pre-computed snapshot of the project's current state for use in AI features."
              + " Served from Redis cache when available (90-minute TTL).")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'READ')")
  public ResponseEntity<ProjectSnapshotDTO> getSnapshot(@PathVariable Long projectId) {
    ProjectSnapshotDTO dto = projectSnapshotService.getOrCompute(projectId);
    return ResponseEntity.ok(dto);
  }

  /**
   * Forces a cache eviction and re-computes the snapshot immediately.
   *
   * <p>Allowed roles: principals with EXECUTE access on AI_FEATURES (PM and above).
   */
  @PostMapping("/{projectId}/snapshot/refresh")
  @Operation(
      summary = "Refresh project AI snapshot",
      description =
          "Evicts the cached snapshot and recomputes it from current data."
              + " Use after major project mutations (cycle closed, initiative added, etc.).")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'EXECUTE')")
  public ResponseEntity<ProjectSnapshotDTO> refreshSnapshot(@PathVariable Long projectId) {
    ProjectSnapshotDTO dto = projectSnapshotService.refresh(projectId);
    return ResponseEntity.ok(dto);
  }
}
