package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.service.PitchDependencyService;
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

/**
 * REST controller for managing pitch dependencies. Provides endpoints for
 * creating, deleting, and querying pitch dependencies.
 */
@RestController
@RequestMapping("/api/pitches/{pitchId}/dependencies")
@RequiredArgsConstructor
@Tag(name = "Pitch Dependencies", description = "Roadmap-level pitch dependency tracking API")
public class PitchDependencyController {

  private final PitchDependencyService pitchDependencyService;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Add a pitch dependency", description = "Create a dependency between the specified pitch and another pitch. Validates against circular dependencies.")
  public ResponseEntity<PitchDependencyDTO> addDependency(@PathVariable Long pitchId,
      @Valid @RequestBody CreatePitchDependencyRequest request) {
    PitchDependencyDTO dependency = pitchDependencyService.addDependency(pitchId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dependency);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all dependencies for a pitch", description = "Returns both blocking and blocked-by dependencies for the specified pitch")
  public ResponseEntity<Map<String, List<PitchDependencyDTO>>> getDependencies(@PathVariable Long pitchId) {
    Map<String, List<PitchDependencyDTO>> dependencies = pitchDependencyService.getDependencies(pitchId);
    return ResponseEntity.ok(dependencies);
  }

  @DeleteMapping("/{dependencyId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Remove a pitch dependency", description = "Delete a specific dependency relationship between pitches")
  public ResponseEntity<Void> removeDependency(@PathVariable Long pitchId, @PathVariable Long dependencyId) {
    pitchDependencyService.removeDependency(dependencyId);
    return ResponseEntity.noContent().build();
  }
}
