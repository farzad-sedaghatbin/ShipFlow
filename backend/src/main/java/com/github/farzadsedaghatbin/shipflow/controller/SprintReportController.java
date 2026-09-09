package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.SprintReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.SprintReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles/{cycleId}/sprint-report")
@RequiredArgsConstructor
@Tag(name = "Sprint Report", description = "Scrum per-sprint summary report")
public class SprintReportController {

  private final SprintReportService sprintReportService;
  private final ProjectService projectService;
  private final CycleRepository cycleRepository;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(
      summary = "Get sprint report for a sprint",
      description =
          "Returns a per-sprint summary: planned vs completed story points, completion rate,"
              + " task counts by status, and scope added after the sprint started."
              + " Project-scope authorization is enforced at the controller layer.")
  public ResponseEntity<SprintReportDTO> getSprintReport(@PathVariable Long cycleId) {
    Cycle cycle =
        cycleRepository
            .findByIdWithProject(cycleId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
    if (cycle.getProject() != null) {
      projectService.requireProjectAccess(cycle.getProject().getId());
    }
    // Pass the already-loaded cycle so SprintReportService does not issue a second DB query.
    return ResponseEntity.ok(sprintReportService.computeSprintReport(cycle));
  }
}
