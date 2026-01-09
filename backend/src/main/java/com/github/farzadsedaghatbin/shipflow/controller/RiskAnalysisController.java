package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionResponse;
import com.github.farzadsedaghatbin.shipflow.service.RiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI-powered risk analysis endpoints.
 * 
 * Fast endpoints (default) use rule-based analysis for quick responses.
 * AI-enhanced endpoints (suffix /ai) use AI for deeper insights but are slower.
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Analysis", description = "Risk analysis for pitches and cycles (fast rule-based + optional AI)")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    @GetMapping("/status")
    @Operation(summary = "Check AI risk analysis status", 
               description = "Returns whether AI risk analysis is enabled and available")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        boolean aiAvailable = riskAnalysisService.isAIAvailable();
        return ResponseEntity.ok(Map.of(
                "enabled", aiAvailable,
                "message", aiAvailable ? 
                        "AI risk analysis is active" : 
                        "AI risk analysis is disabled or AI provider is not available"
        ));
    }

    @GetMapping("/pitch/{pitchId}")
    @Operation(summary = "Analyze pitch risk (fast)", 
               description = "Get rule-based risk analysis for a pitch (fast response)")
    public ResponseEntity<PitchRiskDTO> analyzePitchRisk(@PathVariable Long pitchId) {
        PitchRiskDTO risk = riskAnalysisService.analyzePitchRisk(pitchId, false);
        return ResponseEntity.ok(risk);
    }

    @GetMapping("/pitch/{pitchId}/ai")
    @Operation(summary = "Analyze pitch risk with AI", 
               description = "Get AI-powered risk analysis including score, factors, and recommendations (slower)")
    public ResponseEntity<PitchRiskDTO> analyzePitchRiskWithAI(@PathVariable Long pitchId) {
        PitchRiskDTO risk = riskAnalysisService.analyzePitchRisk(pitchId, true);
        return ResponseEntity.ok(risk);
    }

    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get cycle risk overview (fast)", 
               description = "Get rule-based risk overview for a cycle (fast response)")
    public ResponseEntity<CycleRiskOverviewDTO> getCycleRiskOverview(@PathVariable Long cycleId) {
        CycleRiskOverviewDTO overview = riskAnalysisService.getCycleRiskOverview(cycleId, false);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/cycle/{cycleId}/ai")
    @Operation(summary = "Get cycle risk overview with AI", 
               description = "Get AI-powered risk overview including all pitch risks and aggregated insights (slower)")
    public ResponseEntity<CycleRiskOverviewDTO> getCycleRiskOverviewWithAI(@PathVariable Long cycleId) {
        CycleRiskOverviewDTO overview = riskAnalysisService.getCycleRiskOverview(cycleId, true);
        return ResponseEntity.ok(overview);
    }

    @PostMapping("/pitch/{pitchId}/refresh")
    @Operation(summary = "Refresh pitch risk analysis with AI", 
               description = "Force a fresh AI-powered risk analysis for a pitch")
    public ResponseEntity<PitchRiskDTO> refreshPitchRisk(@PathVariable Long pitchId) {
        PitchRiskDTO risk = riskAnalysisService.analyzePitchRisk(pitchId, true);
        return ResponseEntity.ok(risk);
    }

    @PostMapping("/cycle/{cycleId}/refresh")
    @Operation(summary = "Refresh cycle risk overview with AI", 
               description = "Force a fresh AI-powered risk analysis for an entire cycle")
    public ResponseEntity<CycleRiskOverviewDTO> refreshCycleRisk(@PathVariable Long cycleId) {
        CycleRiskOverviewDTO overview = riskAnalysisService.getCycleRiskOverview(cycleId, true);
        return ResponseEntity.ok(overview);
    }

    @PostMapping("/pitch/{pitchId}/ask")
    @Operation(summary = "Ask AI Risk Advisor a question", 
               description = "Ask a question about the pitch risk analysis and get an AI-powered response")
    public ResponseEntity<RiskQuestionResponse> askRiskQuestion(
            @PathVariable Long pitchId,
            @RequestBody RiskQuestionRequest request) {
        try {
            RiskQuestionResponse response = riskAnalysisService.answerRiskQuestion(pitchId, request.getQuestion());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Fallback error handling
            RiskQuestionResponse errorResponse = RiskQuestionResponse.builder()
                    .pitchId(pitchId)
                    .pitchTitle("Unknown")
                    .question(request.getQuestion())
                    .answer(null)
                    .confidenceScore(0)
                    .aiEnabled(false)
                    .answeredAt(java.time.LocalDateTime.now())
                    .errorMessage("Service error: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }
}
