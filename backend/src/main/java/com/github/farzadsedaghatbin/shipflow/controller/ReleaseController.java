package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateReleaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseProgressDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@Tag(name = "Releases", description = "Release management")
public class ReleaseController {

  private final ReleaseService releaseService;

  @GetMapping("/project/{projectId}")
  @Operation(summary = "Get all releases for a project")
  public ResponseEntity<List<ReleaseDTO>> getReleasesByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(releaseService.getReleasesByProjectId(projectId));
  }

  @GetMapping("/project/{projectId}/upcoming")
  @Operation(summary = "Get upcoming releases for a project")
  public ResponseEntity<List<ReleaseDTO>> getUpcomingReleases(@PathVariable Long projectId) {
    return ResponseEntity.ok(releaseService.getUpcomingReleases(projectId));
  }

  @GetMapping("/project/{projectId}/released")
  @Operation(summary = "Get past releases for a project")
  public ResponseEntity<List<ReleaseDTO>> getReleasedReleases(@PathVariable Long projectId) {
    return ResponseEntity.ok(releaseService.getReleasedReleases(projectId));
  }

  @GetMapping("/project/{projectId}/status/{status}")
  @Operation(summary = "Get releases by project and status")
  public ResponseEntity<List<ReleaseDTO>> getReleasesByProjectAndStatus(
      @PathVariable Long projectId,
      @PathVariable ReleaseStatus status) {
    return ResponseEntity.ok(releaseService.getReleasesByProjectIdAndStatus(projectId, status));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get release by ID")
  public ResponseEntity<ReleaseDTO> getReleaseById(@PathVariable Long id) {
    return ResponseEntity.ok(releaseService.getReleaseById(id));
  }

  @GetMapping("/{id}/progress")
  @Operation(summary = "Get release progress with scope metrics")
  public ResponseEntity<ReleaseProgressDTO> getReleaseProgress(@PathVariable Long id) {
    return ResponseEntity.ok(releaseService.calculateProgress(id));
  }

  @PostMapping
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.CREATE)
  @Operation(summary = "Create a new release")
  public ResponseEntity<ReleaseDTO> createRelease(@Valid @RequestBody CreateReleaseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(releaseService.createRelease(request));
  }

  @PutMapping("/{id}")
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update a release")
  public ResponseEntity<ReleaseDTO> updateRelease(
      @PathVariable Long id,
      @Valid @RequestBody CreateReleaseRequest request) {
    return ResponseEntity.ok(releaseService.updateRelease(id, request));
  }

  @PatchMapping("/{id}/status")
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update release status")
  public ResponseEntity<ReleaseDTO> updateReleaseStatus(
      @PathVariable Long id,
      @RequestParam ReleaseStatus status) {
    return ResponseEntity.ok(releaseService.updateStatus(id, status));
  }

  @PatchMapping("/{id}/link-cycle")
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.UPDATE)
  @Operation(summary = "Link a cycle to a release")
  public ResponseEntity<ReleaseDTO> linkCycle(
      @PathVariable Long id,
      @RequestParam Long cycleId) {
    return ResponseEntity.ok(releaseService.linkCycle(id, cycleId));
  }

  @PatchMapping("/{id}/unlink-cycle")
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.UPDATE)
  @Operation(summary = "Unlink a cycle from a release")
  public ResponseEntity<ReleaseDTO> unlinkCycle(
      @PathVariable Long id,
      @RequestParam Long cycleId) {
    return ResponseEntity.ok(releaseService.unlinkCycle(id, cycleId));
  }

  @DeleteMapping("/{id}")
  @RequirePermission(resource = ResourceType.RELEASE, permission = PermissionType.DELETE)
  @Operation(summary = "Delete a release (soft delete)")
  public ResponseEntity<Void> deleteRelease(@PathVariable Long id) {
    releaseService.deleteRelease(id);
    return ResponseEntity.noContent().build();
  }
}
