package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.betting.*;
import com.github.farzadsedaghatbin.shipflow.service.BettingDecisionService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing betting decisions.
 * Supports recording commit/reject decisions with reasoning and viewing decision history.
 */
@RestController
@RequestMapping("/api/betting/decisions")
@RequiredArgsConstructor
@Tag(name = "Betting Decisions", description = "Track and manage betting decisions for Shape Up methodology")
public class BettingDecisionController {

  private final BettingDecisionService bettingDecisionService;
  private final UserService userService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(
      summary = "Record a betting decision",
      description = "Record a commit/reject/defer decision for a pitch in a cycle with reasoning. "
          + "This creates a historical record of why betting decisions were made.")
  public ResponseEntity<BettingDecisionDTO> recordDecision(
      @Valid @RequestBody RecordBettingDecisionRequest request) {
    Long userId = getCurrentUserId();
    BettingDecisionDTO decision = bettingDecisionService.recordDecision(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(decision);
  }

  @GetMapping("/pitch/{pitchId}/history")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get pitch decision history",
      description = "Get all betting decisions made for a pitch across all cycles. "
          + "Useful for understanding why a pitch has been repeatedly rejected or deferred.")
  public ResponseEntity<List<BettingDecisionDTO>> getPitchHistory(@PathVariable Long pitchId) {
    return ResponseEntity.ok(bettingDecisionService.getPitchDecisionHistory(pitchId));
  }

  @GetMapping("/pitch/{pitchId}/latest")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get most recent decision for a pitch",
      description = "Returns the most recent betting decision made for a pitch.")
  public ResponseEntity<BettingDecisionDTO> getMostRecentDecision(@PathVariable Long pitchId) {
    Optional<BettingDecisionDTO> decision = bettingDecisionService.getMostRecentDecision(pitchId);
    return decision.map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/cycle/{cycleId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get cycle decisions",
      description = "Get all betting decisions for a specific cycle.")
  public ResponseEntity<List<BettingDecisionDTO>> getCycleDecisions(@PathVariable Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.getCycleDecisions(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/history")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get comprehensive cycle decision history",
      description = "Get detailed decision history with statistics including decision counts, "
          + "repeatedly rejected pitches, and appetite analysis.")
  public ResponseEntity<BettingDecisionHistoryDTO> getCycleDecisionHistory(@PathVariable Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.getCycleDecisionHistory(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/committed")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get committed decisions for a cycle",
      description = "Get only the pitches that were committed to for a cycle.")
  public ResponseEntity<List<BettingDecisionDTO>> getCommittedDecisions(@PathVariable Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.getCommittedDecisions(cycleId));
  }

  @GetMapping("/cycle/{cycleId}/rejected")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get rejected decisions for a cycle",
      description = "Get pitches that were rejected for a cycle with their rejection reasons.")
  public ResponseEntity<List<BettingDecisionDTO>> getRejectedDecisions(@PathVariable Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.getRejectedDecisions(cycleId));
  }

  @GetMapping("/pitch/{pitchId}/exists")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Check if decision exists",
      description = "Check if a decision has already been recorded for a pitch in a cycle.")
  public ResponseEntity<Boolean> hasDecision(
      @PathVariable Long pitchId,
      @RequestParam Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.hasDecisionForPitchInCycle(pitchId, cycleId));
  }

  @GetMapping("/project/{projectId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get project decisions (paginated)",
      description = "Get all betting decisions for a project across all cycles, paginated.")
  public ResponseEntity<Page<BettingDecisionDTO>> getProjectDecisions(
      @PathVariable Long projectId,
      Pageable pageable) {
    return ResponseEntity.ok(bettingDecisionService.getProjectDecisions(projectId, pageable));
  }

  // === Appetite Comparison Endpoints ===

  @GetMapping("/compare/pitch/{pitchId}/team/{teamId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Compare pitch appetite vs team capacity",
      description = "Analyze if a pitch's appetite fits within a team's available capacity. "
          + "Returns fit assessment and recommendations.")
  public ResponseEntity<AppetiteComparisonDTO> compareAppetite(
      @PathVariable Long pitchId,
      @PathVariable Long teamId,
      @RequestParam Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.compareAppetite(pitchId, teamId, cycleId));
  }

  @GetMapping("/compare/pitch/{pitchId}/all-teams")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Compare pitch appetite against all teams",
      description = "Analyze a pitch against all teams in a cycle to find the best fit. "
          + "Returns sorted list by utilization percentage.")
  public ResponseEntity<List<AppetiteComparisonDTO>> compareAppetiteAllTeams(
      @PathVariable Long pitchId,
      @RequestParam Long cycleId) {
    return ResponseEntity.ok(bettingDecisionService.compareAppetiteAllTeams(pitchId, cycleId));
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
