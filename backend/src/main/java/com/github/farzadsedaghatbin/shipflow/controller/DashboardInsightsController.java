package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardInsightsResponseDTO;
import com.github.farzadsedaghatbin.shipflow.service.DashboardInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Proactive Dashboard Insights feature.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code GET  /api/ai/dashboard/{projectId}/insights} – fetch cached insights (60-min TTL)
 *   <li>{@code POST /api/ai/dashboard/{projectId}/insights/refresh} – evict cache and re-compute
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Insights", description = "Proactive AI-powered project health alerts")
public class DashboardInsightsController {

  private final DashboardInsightsService dashboardInsightsService;

  /**
   * Return cached insights for the given project.
   *
   * <p>All authenticated roles including VIEWER may read insights.
   */
  @GetMapping("/{projectId}/insights")
  @Operation(
      summary = "Get proactive dashboard insights",
      description =
          "Returns AI-powered health alerts for a project (overdue pitches, at-risk cycles,"
              + " scope creep, velocity trend). Results are cached in Redis for 60 minutes.")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'READ')")
  public ResponseEntity<DashboardInsightsResponseDTO> getInsights(
      @PathVariable Long projectId) {
    return ResponseEntity.ok(dashboardInsightsService.getInsights(projectId));
  }

  /**
   * Evict the cache and re-compute insights for the given project.
   *
   * <p>VIEWER role excluded from cache-busting to prevent abuse.
   */
  @PostMapping("/{projectId}/insights/refresh")
  @Operation(
      summary = "Refresh dashboard insights",
      description =
          "Evicts the Redis cache entry for this project and re-computes insights immediately."
              + " Use sparingly — the regular GET endpoint is cached for 60 minutes.")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'EXECUTE')")
  public ResponseEntity<DashboardInsightsResponseDTO> refreshInsights(
      @PathVariable Long projectId) {
    return ResponseEntity.ok(dashboardInsightsService.refreshInsights(projectId));
  }
}
