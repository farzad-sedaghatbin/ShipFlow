package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateInitiativeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.InitiativeDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.InitiativeService;
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
@RequestMapping("/api/initiatives")
@RequiredArgsConstructor
@Tag(name = "Initiatives", description = "Strategic initiative management")
public class InitiativeController {

  private final InitiativeService initiativeService;

  @GetMapping("/project/{projectId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all initiatives for a project")
  public ResponseEntity<List<InitiativeDTO>> getInitiativesByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(initiativeService.getInitiativesByProjectId(projectId));
  }

  @GetMapping("/project/{projectId}/status/{status}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get initiatives by project and status")
  public ResponseEntity<List<InitiativeDTO>> getInitiativesByProjectAndStatus(
      @PathVariable Long projectId,
      @PathVariable InitiativeStatus status) {
    return ResponseEntity.ok(initiativeService.getInitiativesByProjectIdAndStatus(projectId, status));
  }

  @GetMapping("/project/{projectId}/active")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get active initiatives for a project (PLANNED or IN_PROGRESS)")
  public ResponseEntity<List<InitiativeDTO>> getActiveInitiatives(@PathVariable Long projectId) {
    return ResponseEntity.ok(initiativeService.getActiveInitiatives(projectId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get initiative by ID")
  public ResponseEntity<InitiativeDTO> getInitiativeById(@PathVariable Long id) {
    return ResponseEntity.ok(initiativeService.getInitiativeById(id));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.CREATE)
  @Operation(summary = "Create a new initiative")
  public ResponseEntity<InitiativeDTO> createInitiative(@Valid @RequestBody CreateInitiativeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(initiativeService.createInitiative(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update an initiative")
  public ResponseEntity<InitiativeDTO> updateInitiative(
      @PathVariable Long id,
      @Valid @RequestBody CreateInitiativeRequest request) {
    return ResponseEntity.ok(initiativeService.updateInitiative(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update initiative status")
  public ResponseEntity<InitiativeDTO> updateInitiativeStatus(
      @PathVariable Long id,
      @RequestParam InitiativeStatus status) {
    return ResponseEntity.ok(initiativeService.updateStatus(id, status));
  }

  @PatchMapping("/{id}/dates")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update initiative target dates")
  public ResponseEntity<InitiativeDTO> updateInitiativeDates(
      @PathVariable Long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return ResponseEntity.ok(initiativeService.updateDates(id, startDate, endDate));
  }

  @PatchMapping("/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.UPDATE)
  @Operation(summary = "Reorder initiatives (batch update sort order)")
  public ResponseEntity<Void> reorderInitiatives(@Valid @RequestBody ReorderRequest request) {
    initiativeService.reorder(request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @RequirePermission(resource = ResourceType.INITIATIVE, permission = PermissionType.DELETE)
  @Operation(summary = "Delete an initiative (soft delete)")
  public ResponseEntity<Void> deleteInitiative(@PathVariable Long id) {
    initiativeService.deleteInitiative(id);
    return ResponseEntity.noContent().build();
  }
}
