package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.CreateRiskFeedbackRequest;
import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO;
import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackStatsDTO;
import com.github.farzadsedaghatbin.shipflow.service.RiskFeedbackService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI risk feedback endpoints. Allows team leads to provide
 * feedback on AI risk assessments.
 */
@RestController
@RequestMapping("/api/risk/feedback")
@RequiredArgsConstructor
@Tag(name = "Risk Feedback", description = "Submit and view feedback on AI risk assessments")
public class RiskFeedbackController {

  private final RiskFeedbackService riskFeedbackService;
  private final UserService userService;

  @PostMapping
  @Operation(summary = "Submit risk feedback", description = "Submit feedback on an AI risk assessment to help improve accuracy")
  public ResponseEntity<RiskFeedbackDTO> submitFeedback(@Valid @RequestBody CreateRiskFeedbackRequest request) {
    Long userId = getCurrentUserId();
    RiskFeedbackDTO feedback = riskFeedbackService.submitFeedback(request, userId);
    return ResponseEntity.ok(feedback);
  }

  @GetMapping("/pitch/{pitchId}")
  @Operation(summary = "Get feedback for pitch", description = "Get all feedback entries for a specific pitch")
  public ResponseEntity<List<RiskFeedbackDTO>> getFeedbackByPitch(@PathVariable Long pitchId) {
    List<RiskFeedbackDTO> feedback = riskFeedbackService.getFeedbackByPitch(pitchId);
    return ResponseEntity.ok(feedback);
  }

  @GetMapping("/cycle/{cycleId}")
  @Operation(summary = "Get feedback for cycle", description = "Get all feedback entries for a specific cycle")
  public ResponseEntity<List<RiskFeedbackDTO>> getFeedbackByCycle(@PathVariable Long cycleId) {
    List<RiskFeedbackDTO> feedback = riskFeedbackService.getFeedbackByCycle(cycleId);
    return ResponseEntity.ok(feedback);
  }

  @GetMapping("/stats")
  @Operation(summary = "Get feedback statistics", description = "Get aggregated statistics on AI risk assessment accuracy")
  public ResponseEntity<RiskFeedbackStatsDTO> getFeedbackStats() {
    RiskFeedbackStatsDTO stats = riskFeedbackService.getFeedbackStats();
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/recent")
  @Operation(summary = "Get recent feedback", description = "Get feedback from the last 30 days")
  public ResponseEntity<List<RiskFeedbackDTO>> getRecentFeedback() {
    List<RiskFeedbackDTO> feedback = riskFeedbackService.getRecentFeedback();
    return ResponseEntity.ok(feedback);
  }

  @GetMapping("/pitch/{pitchId}/check")
  @Operation(summary = "Check if user submitted feedback", description = "Check if current user has already submitted feedback for a pitch")
  public ResponseEntity<Map<String, Boolean>> checkUserFeedback(@PathVariable Long pitchId) {
    Long userId = getCurrentUserId();
    boolean hasSubmitted = riskFeedbackService.hasUserSubmittedFeedback(pitchId, userId);
    return ResponseEntity.ok(Map.of("hasSubmitted", hasSubmitted));
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
