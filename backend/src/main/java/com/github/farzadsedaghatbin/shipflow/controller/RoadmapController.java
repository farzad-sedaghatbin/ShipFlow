package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.security.RequirePermission;
import com.github.farzadsedaghatbin.shipflow.service.RoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Tag(name = "Roadmap", description = "Roadmap and timeline views")
public class RoadmapController {

  private final RoadmapService roadmapService;

  @GetMapping("/project/{projectId}/timeline")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.PROJECT, permission = PermissionType.READ)
  @Operation(summary = "Get roadmap timeline for a date range")
  public ResponseEntity<RoadmapTimelineDTO> getRoadmapTimeline(
      @PathVariable Long projectId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return ResponseEntity.ok(roadmapService.getRoadmapTimeline(projectId, startDate, endDate));
  }

  @GetMapping("/project/{projectId}/quarterly")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.PROJECT, permission = PermissionType.READ)
  @Operation(summary = "Get quarterly roadmap view")
  public ResponseEntity<RoadmapTimelineDTO> getQuarterlyRoadmap(
      @PathVariable Long projectId,
      @RequestParam int year,
      @RequestParam int quarter) {
    return ResponseEntity.ok(roadmapService.getQuarterlyRoadmap(projectId, year, quarter));
  }

  @GetMapping("/project/{projectId}/yearly")
  @PreAuthorize("isAuthenticated()")
  @RequirePermission(resource = ResourceType.PROJECT, permission = PermissionType.READ)
  @Operation(summary = "Get yearly roadmap view")
  public ResponseEntity<RoadmapTimelineDTO> getYearlyRoadmap(
      @PathVariable Long projectId,
      @RequestParam int year) {
    return ResponseEntity.ok(roadmapService.getYearlyRoadmap(projectId, year));
  }
}
