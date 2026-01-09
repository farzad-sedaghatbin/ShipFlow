package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
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
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
@Tag(name = "Cycles", description = "Shape Up cycle management")
public class CycleController {

    private final CycleService cycleService;

    @GetMapping
    @Operation(summary = "Get all cycles")
    public ResponseEntity<List<CycleDTO>> getAllCycles() {
        return ResponseEntity.ok(cycleService.getAllCycles());
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a new cycle")
    public ResponseEntity<CycleDTO> createCycle(@Valid @RequestBody CreateCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cycleService.createCycle(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update a cycle")
    public ResponseEntity<CycleDTO> updateCycle(@PathVariable Long id, @Valid @RequestBody CreateCycleRequest request) {
        return ResponseEntity.ok(cycleService.updateCycle(id, request));
    }

    @PatchMapping("/{id}/phase")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update cycle phase")
    public ResponseEntity<CycleDTO> updatePhase(@PathVariable Long id, @RequestParam CyclePhase phase) {
        return ResponseEntity.ok(cycleService.updatePhase(id, phase));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Toggle cycle active status")
    public ResponseEntity<CycleDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.toggleActive(id));
    }

    @PostMapping("/{id}/close")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a cycle")
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.deleteCycle(id);
        return ResponseEntity.noContent().build();
    }
}
