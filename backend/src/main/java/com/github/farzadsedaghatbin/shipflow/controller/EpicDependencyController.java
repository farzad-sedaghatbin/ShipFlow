package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.service.EpicDependencyService;
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
 * REST controller for managing epic dependencies. Epic dependencies are informational
 * only (no hard reorder enforcement) — used for roadmap visualisation.
 */
@RestController
@RequestMapping("/api/epics/{epicId}/dependencies")
@RequiredArgsConstructor
@Tag(name = "Epic Dependencies", description = "Roadmap-level epic dependency tracking API (informational)")
public class EpicDependencyController {

  private final EpicDependencyService epicDependencyService;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Add an epic dependency", description = "Create an informational dependency between the specified epic and another epic. Validates against circular dependencies.")
  public ResponseEntity<EpicDependencyDTO> addDependency(@PathVariable Long epicId,
      @Valid @RequestBody CreateEpicDependencyRequest request) {
    EpicDependencyDTO dependency = epicDependencyService.addDependency(epicId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dependency);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all dependencies for an epic", description = "Returns both blocking and blocked-by dependencies for the specified epic")
  public ResponseEntity<Map<String, List<EpicDependencyDTO>>> getDependencies(@PathVariable Long epicId) {
    Map<String, List<EpicDependencyDTO>> dependencies = epicDependencyService.getDependencies(epicId);
    return ResponseEntity.ok(dependencies);
  }

  @DeleteMapping("/{dependencyId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Remove an epic dependency", description = "Delete a specific dependency relationship between epics")
  public ResponseEntity<Void> removeDependency(@PathVariable Long epicId, @PathVariable Long dependencyId) {
    epicDependencyService.removeDependency(dependencyId);
    return ResponseEntity.noContent().build();
  }
}
