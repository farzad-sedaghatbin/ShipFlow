package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.VelocityPointDTO;
import com.github.farzadsedaghatbin.shipflow.service.VelocityService;
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
@RequestMapping("/api/projects/{projectId}/velocity")
@RequiredArgsConstructor
@Tag(name = "Velocity", description = "Scrum sprint velocity chart data")
public class VelocityController {

  private final VelocityService velocityService;

  @GetMapping
  @PreAuthorize(
      "hasAnyRole('ADMIN','PROJECT_MANAGER','DEVELOPER','QA','PRODUCT','VIEWER')")
  @Operation(
      summary = "Get velocity chart data for a project",
      description =
          "Returns planned and completed story points per sprint/cycle for the given project,"
              + " ordered by sprint start date ascending.")
  public ResponseEntity<List<VelocityPointDTO>> getVelocity(@PathVariable Long projectId) {
    return ResponseEntity.ok(velocityService.computeVelocity(projectId));
  }
}
