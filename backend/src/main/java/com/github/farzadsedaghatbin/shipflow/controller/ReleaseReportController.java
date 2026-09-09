package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.ReleaseReportDTO;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.ReleaseReportService;
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
@RequestMapping("/api/projects/{projectId}/release-report")
@RequiredArgsConstructor
@Tag(name = "Release Report", description = "Scrum release report data")
public class ReleaseReportController {

  private final ReleaseReportService releaseReportService;
  private final ProjectService projectService;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('BACKLOG', 'READ')")
  @Operation(
      summary = "Get release report data for a project",
      description =
          "Returns task counts and planned/completed story points per release for the given"
              + " project.")
  public ResponseEntity<List<ReleaseReportDTO>> getReleaseReport(@PathVariable Long projectId) {
    projectService.requireProjectAccess(projectId);
    return ResponseEntity.ok(releaseReportService.computeReleaseReport(projectId));
  }
}
