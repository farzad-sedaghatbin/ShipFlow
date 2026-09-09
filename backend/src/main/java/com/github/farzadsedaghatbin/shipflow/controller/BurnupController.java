package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.BurnupPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.BurnupService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles/{cycleId}/burnup")
@RequiredArgsConstructor
@Tag(name = "Burnup", description = "Scrum sprint burnup chart data")
public class BurnupController {

  private final BurnupService burnupService;
  private final ProjectService projectService;
  private final CycleRepository cycleRepository;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(
      summary = "Get burnup chart data for a sprint",
      description =
          "Returns daily cumulative completed story points and the total sprint scope from"
              + " sprint start to today (capped at sprint end date). Only tasks with story"
              + " points are included. Project-scope authorization is enforced at the"
              + " controller layer.")
  public ResponseEntity<List<BurnupPointDTO>> getBurnup(@PathVariable Long cycleId) {
    Cycle cycle =
        cycleRepository
            .findByIdWithProject(cycleId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
    if (cycle.getProject() != null) {
      projectService.requireProjectAccess(cycle.getProject().getId());
    }
    // Pass the already-loaded cycle so BurnupService does not issue a second DB query.
    return ResponseEntity.ok(burnupService.computeBurnup(cycle));
  }
}
