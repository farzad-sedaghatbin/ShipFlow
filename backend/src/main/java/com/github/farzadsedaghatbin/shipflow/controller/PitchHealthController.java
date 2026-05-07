package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO;
import com.github.farzadsedaghatbin.shipflow.service.PitchHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for pitch health summary endpoints. Designed for
 * non-technical stakeholders (PMs, founders).
 *
 * <p>
 * Fast endpoints (default) use rule-based analysis for quick responses.
 * AI-enhanced endpoints use AI for deeper insights but are slower.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Pitch Health", description = "Simplified health summaries for stakeholders")
public class PitchHealthController {

  private final PitchHealthService pitchHealthService;

  @GetMapping("/pitch/{pitchId}")
  @Operation(summary = "Get pitch health summary (fast)", description = "Returns a simplified health snapshot for a single pitch using rule-based analysis")
  public ResponseEntity<PitchHealthDTO> getPitchHealth(@PathVariable Long pitchId) {
    PitchHealthDTO health = pitchHealthService.getPitchHealth(pitchId, false);
    return ResponseEntity.ok(health);
  }

  @GetMapping("/pitch/{pitchId}/ai")
  @Operation(summary = "Get pitch health summary with AI", description = "Returns health snapshot with AI-powered risk analysis (slower)")
  public ResponseEntity<PitchHealthDTO> getPitchHealthWithAI(@PathVariable Long pitchId) {
    PitchHealthDTO health = pitchHealthService.getPitchHealth(pitchId, true);
    return ResponseEntity.ok(health);
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get cycle health summary (fast)", description = "Returns aggregated health summary for an entire cycle using rule-based analysis")
  public ResponseEntity<CycleHealthSummaryDTO> getCycleHealth(@PathVariable Long cycleId) {
    CycleHealthSummaryDTO summary = pitchHealthService.getCycleHealthSummary(cycleId, false);
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/cycle/{cycleId}/ai")
  @Operation(summary = "Get cycle health summary with AI", description = "Returns health summary with AI-powered risk analysis for all pitches (slower)")
  public ResponseEntity<CycleHealthSummaryDTO> getCycleHealthWithAI(@PathVariable Long cycleId) {
    CycleHealthSummaryDTO summary = pitchHealthService.getCycleHealthSummary(cycleId, true);
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/active-cycles")
  @Operation(summary = "Get all active cycles health (fast)", description = "Returns health summaries for all active cycles using rule-based analysis")
  public ResponseEntity<List<CycleHealthSummaryDTO>> getAllActiveCycleHealth() {
    List<CycleHealthSummaryDTO> summaries = pitchHealthService.getAllActiveCycleHealth(false);
    return ResponseEntity.ok(summaries);
  }

  @GetMapping("/active-cycles/ai")
  @Operation(summary = "Get all active cycles health with AI", description = "Returns health summaries with AI-powered risk analysis (slower)")
  public ResponseEntity<List<CycleHealthSummaryDTO>> getAllActiveCycleHealthWithAI() {
    List<CycleHealthSummaryDTO> summaries = pitchHealthService.getAllActiveCycleHealth(true);
    return ResponseEntity.ok(summaries);
  }

  @GetMapping("/cycle/{cycleId}/stagnation")
  @Operation(summary = "Detect stagnating pitches in cycle", 
      description = "Returns alerts for pitches showing signs of stagnation (no progress, stuck at peak, etc.)")
  public ResponseEntity<List<StagnationAlertDTO>> getStagnationAlerts(@PathVariable Long cycleId) {
    List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(cycleId);
    return ResponseEntity.ok(alerts);
  }

  @GetMapping("/stagnation/all")
  @Operation(summary = "Detect all stagnating pitches", 
      description = "Returns stagnation alerts for all pitches in active cycles")
  public ResponseEntity<List<StagnationAlertDTO>> getAllStagnationAlerts() {
    List<StagnationAlertDTO> alerts = pitchHealthService.detectAllStagnatingPitches();
    return ResponseEntity.ok(alerts);
  }
}
