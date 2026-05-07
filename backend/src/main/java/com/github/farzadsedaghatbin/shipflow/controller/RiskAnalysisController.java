package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionResponse;
import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import com.github.farzadsedaghatbin.shipflow.service.RiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered risk analysis endpoints.
 *
 * <p>
 * Fast endpoints (default) use rule-based analysis for quick responses.
 * AI-enhanced endpoints (suffix /ai) use AI for deeper insights but are slower.
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Analysis", description = "Risk analysis for pitches and cycles (fast rule-based). For AI analysis, use /api/risk/async/* endpoints.")
public class RiskAnalysisController {

  private final RiskAnalysisService riskAnalysisService;

  @GetMapping("/status")
  @Operation(summary = "Check AI risk analysis status", description = "Returns whether AI risk analysis is enabled and available")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<Map<String, Object>> getAIStatus() {
    boolean aiAvailable = riskAnalysisService.isAIAvailable();
    return ResponseEntity.ok(Map.of("enabled", aiAvailable, "message",
        aiAvailable
            ? "AI risk analysis is active. Use /api/risk/async/* for AI-powered analysis."
            : "AI risk analysis is disabled or AI provider is not available"));
  }

  @GetMapping("/pitch/{pitchId}")
  @Operation(summary = "Analyze pitch risk (fast)", description = "Get rule-based risk analysis for a pitch (fast response). For AI analysis, use POST /api/risk/async/pitch/{pitchId}/analyze")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<PitchRiskDTO> analyzePitchRisk(@PathVariable Long pitchId) {
    PitchRiskDTO risk = riskAnalysisService.analyzePitchRisk(pitchId, false);
    return ResponseEntity.ok(risk);
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get cycle risk overview (fast)", description = "Get rule-based risk overview for a cycle (fast response). For AI analysis, use POST /api/risk/async/cycle/{cycleId}/analyze")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<CycleRiskOverviewDTO> getCycleRiskOverview(@PathVariable Long cycleId) {
    CycleRiskOverviewDTO overview = riskAnalysisService.getCycleRiskOverview(cycleId, false);
    return ResponseEntity.ok(overview);
  }

  @PostMapping("/pitch/{pitchId}/ask")
  @Operation(summary = "Ask AI Risk Advisor a question", description = "Ask a question about the pitch risk analysis and get an AI-powered response")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<RiskQuestionResponse> askRiskQuestion(@PathVariable Long pitchId,
      @RequestBody RiskQuestionRequest request) {
    try {
      RiskQuestionResponse response = riskAnalysisService.answerRiskQuestion(pitchId, request.getQuestion());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      // Fallback error handling
      RiskQuestionResponse errorResponse = RiskQuestionResponse.builder().pitchId(pitchId).pitchTitle("Unknown")
          .question(request.getQuestion()).answer(null).confidenceScore(0).aiEnabled(false)
          .answeredAt(java.time.LocalDateTime.now()).errorMessage("Service error: " + e.getMessage()).build();
      return ResponseEntity.ok(errorResponse);
    }
  }

  @GetMapping("/pitch/{pitchId}/history")
  @Operation(summary = "Get pitch risk history", description = "Retrieve historical risk scores for trend analysis")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<List<PitchRiskHistory>> getPitchRiskHistory(@PathVariable Long pitchId,
      @RequestParam(required = false, defaultValue = "30") Integer days) {
    List<PitchRiskHistory> history = riskAnalysisService.getRiskHistory(pitchId, days);
    return ResponseEntity.ok(history);
  }
}
