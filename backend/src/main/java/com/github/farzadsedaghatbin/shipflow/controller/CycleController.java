package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
@Tag(name = "Cycles", description = "Shape Up cycle management")
public class CycleController {

  private final CycleService cycleService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all cycles (Admin only)")
  public ResponseEntity<List<CycleDTO>> getAllCycles() {
    return ResponseEntity.ok(cycleService.getAllCycles());
  }

  @GetMapping("/my-cycles")
  @Operation(summary = "Get cycles accessible to current user")
  public ResponseEntity<List<CycleDTO>> getMyCycles() {
    return ResponseEntity.ok(cycleService.getAccessibleCycles());
  }

  @GetMapping("/my-cycles/active")
  @Operation(summary = "Get active cycles accessible to current user")
  public ResponseEntity<List<CycleDTO>> getMyActiveCycles() {
    return ResponseEntity.ok(cycleService.getAccessibleActiveCycles());
  }

  @GetMapping("/project/{projectId}")
  @Operation(summary = "Get cycles by project")
  public ResponseEntity<List<CycleDTO>> getCyclesByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(cycleService.getCyclesByProject(projectId));
  }

  @GetMapping("/project/{projectId}/active")
  @Operation(summary = "Get active cycles by project")
  public ResponseEntity<List<CycleDTO>> getActiveCyclesByProject(@PathVariable Long projectId) {
    return ResponseEntity.ok(cycleService.getActiveCyclesByProject(projectId));
  }

  @GetMapping("/active")
  @Operation(summary = "Get active cycles")
  public ResponseEntity<List<CycleDTO>> getActiveCycles() {
    return ResponseEntity.ok(cycleService.getActiveCycles());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get cycle by ID")
  public ResponseEntity<CycleDTO> getCycleById(@PathVariable Long id) {
    return ResponseEntity.ok(cycleService.getCycleById(id));
  }

  // Security Model: @RequirePermission allows any user with CREATE permission to
  // invoke this
  // endpoint.
  // Role-based validation for custom end dates (ADMIN/PROJECT_MANAGER only) is
  // enforced in
  // CycleService.
  // Users without elevated roles can create cycles with auto-calculated end dates
  // only.
  @PostMapping
  @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.CREATE)
  @Operation(summary = "Create a new cycle")
  public ResponseEntity<CycleDTO> createCycle(@Valid @RequestBody CreateCycleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cycleService.createCycle(request));
  }

  // Security Model: @RequirePermission allows any user with UPDATE permission to
  // invoke this
  // endpoint.
  // Role-based validation for custom end dates (ADMIN/PROJECT_MANAGER only) is
  // enforced in
  // CycleService.
  // Users without elevated roles can update cycles with auto-calculated end dates
  // only.
  @PutMapping("/{id}")
  @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.UPDATE)
  @Operation(summary = "Update a cycle")
  public ResponseEntity<CycleDTO> updateCycle(@PathVariable Long id, @Valid @RequestBody CreateCycleRequest request) {
    return ResponseEntity.ok(cycleService.updateCycle(id, request));
  }

  @PatchMapping("/{id}/toggle-active")
  @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.MANAGE)
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
  @Operation(summary = "Toggle cycle active status")
  public ResponseEntity<CycleDTO> toggleActive(@PathVariable Long id) {
    return ResponseEntity.ok(cycleService.toggleActive(id));
  }

  @PostMapping("/{id}/close")
  @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.MANAGE)
  @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
  @Operation(summary = "Close/complete a cycle (requires at least one closed retrospective)")
  public ResponseEntity<CycleDTO> closeCycle(@PathVariable Long id) {
    return ResponseEntity.ok(cycleService.closeCycle(id));
  }

  @GetMapping("/{id}/retro-status")
  @Operation(summary = "Get cycle retrospective completion status")
  public ResponseEntity<CycleRetroStatusDTO> getCycleRetroStatus(@PathVariable Long id) {
    return ResponseEntity.ok(cycleService.getCycleRetroStatus(id));
  }

  @DeleteMapping("/{id}")
  @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.DELETE)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete a cycle")
  public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
    cycleService.deleteCycle(id);
    return ResponseEntity.noContent().build();
  }
}
