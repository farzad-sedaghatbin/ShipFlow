package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.signal.*;
import com.github.farzadsedaghatbin.shipflow.service.CycleSignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for cycle signals - insights derived from historical data.
 * Part of v0.5 "Insight, Not Metrics" feature set.
 */
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
@Tag(name = "Signals", description = "Cycle signals and insights for decision support")
public class SignalController {

  private final CycleSignalService cycleSignalService;

  @GetMapping("/project/{projectId}")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get all signals for a project",
      description = "Returns aggregated signals across recent cycles including appetite accuracy, shaping patterns, risk correlation, and retro follow-through")
  public ResponseEntity<CycleSignalsDTO> getProjectSignals(
      @PathVariable Long projectId,
      @Parameter(description = "Number of recent cycles to analyze (default: 6)")
      @RequestParam(defaultValue = "6") int cycleCount) {
    return ResponseEntity.ok(cycleSignalService.getProjectSignals(projectId, cycleCount));
  }

  @GetMapping("/cycle/{cycleId}")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get signals for a specific cycle",
      description = "Returns signals for a single cycle with historical trend context")
  public ResponseEntity<CycleSignalsDTO> getCycleSignals(@PathVariable Long cycleId) {
    return ResponseEntity.ok(cycleSignalService.getCycleSignals(cycleId));
  }

  @GetMapping("/project/{projectId}/appetite-accuracy")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get appetite accuracy signal",
      description = "Returns detailed appetite accuracy trend analysis")
  public ResponseEntity<AppetiteAccuracySignalDTO> getAppetiteAccuracySignal(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "6") int cycleCount) {
    var cycles = cycleSignalService.getProjectSignals(projectId, cycleCount);
    return ResponseEntity.ok(cycles.getAppetiteAccuracy());
  }

  @GetMapping("/project/{projectId}/shaping-pattern")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get shaping pattern signal",
      description = "Returns analysis of over/under shaping patterns")
  public ResponseEntity<ShapingPatternSignalDTO> getShapingPatternSignal(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "6") int cycleCount) {
    var cycles = cycleSignalService.getProjectSignals(projectId, cycleCount);
    return ResponseEntity.ok(cycles.getShapingPattern());
  }

  @GetMapping("/project/{projectId}/risk-correlation")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get risk correlation signal",
      description = "Returns analysis of risk prediction accuracy")
  public ResponseEntity<RiskCorrelationSignalDTO> getRiskCorrelationSignal(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "6") int cycleCount) {
    var cycles = cycleSignalService.getProjectSignals(projectId, cycleCount);
    return ResponseEntity.ok(cycles.getRiskCorrelation());
  }

  @GetMapping("/project/{projectId}/retro-follow-through")
  @PreAuthorize("@permissionService.hasPermission('REPORT', 'READ')")
  @Operation(summary = "Get retro follow-through signal",
      description = "Returns analysis of action item follow-through rates")
  public ResponseEntity<RetroFollowThroughSignalDTO> getRetroFollowThroughSignal(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "6") int cycleCount) {
    var cycles = cycleSignalService.getProjectSignals(projectId, cycleCount);
    return ResponseEntity.ok(cycles.getRetroFollowThrough());
  }
}
