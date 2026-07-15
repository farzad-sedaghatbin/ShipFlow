package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskQuestionResponse;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService;
import com.github.farzadsedaghatbin.shipflow.service.AsyncAIAdvisorService.JobStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for async AI advisor operations.
 * 
 * CACHE-FIRST PATTERN:
 * - All endpoints check cache first and return immediately (200 OK) if cached
 * - Only uncached requests go through async job flow (202 Accepted with jobId)
 * 
 * Non-blocking AI analysis through a job-based pattern:
 * 1. Check cache - if hit, return 200 with result immediately
 * 2. If cache miss, start job (POST) - returns 202 with jobId
 * 3. Poll job status (GET /jobs/{jobId}/status)
 * 4. Get result when complete (GET /jobs/{jobId}/result)
 * 
 * This pattern prevents long HTTP timeouts when AI responses are slow or not cached.
 */
@RestController
@RequestMapping("/api/risk/async")
@RequiredArgsConstructor
@Tag(name = "Async AI Advisor", description = "Non-blocking AI advisory operations with cache-first + job-based polling")
public class AsyncAIAdvisorController {

  private final AsyncAIAdvisorService asyncService;

  // ===========================================
  // START ASYNC JOBS (WITH CACHE-FIRST CHECK)
  // ===========================================

  @PostMapping("/pitch/{pitchId}/analyze")
  @Operation(summary = "Start async pitch risk analysis",
      description = "Returns cached result immediately (200) if available, otherwise starts async job (202) and returns jobId for polling. Pass force=true to evict any cached result and always run a fresh AI analysis.")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<?> startPitchRiskAnalysis(
      @PathVariable Long pitchId,
      @RequestParam(defaultValue = "false") boolean force) {
    // CACHE-FIRST: Return immediately if cached (unless a forced refresh was requested)
    if (!force) {
      Optional<PitchRiskDTO> cached = asyncService.getCachedPitchRisk(pitchId);
      if (cached.isPresent()) {
        return ResponseEntity.ok(Map.of(
            "cached", true,
            "result", cached.get()
        ));
      }
    }

    // Cache miss (or forced refresh) - start async job
    String jobId = asyncService.startPitchRiskAnalysis(pitchId, force);
    return ResponseEntity.accepted().body(Map.of(
        "cached", false,
        "jobId", jobId,
        "status", "PENDING",
        "message", "AI analysis started. Poll /jobs/" + jobId + "/status for updates."
    ));
  }

  @PostMapping("/cycle/{cycleId}/analyze")
  @Operation(summary = "Start async cycle risk analysis",
      description = "Returns cached result immediately (200) if available, otherwise starts async job (202) and returns jobId for polling. Pass force=true to evict any cached result and always run a fresh AI analysis.")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<?> startCycleRiskAnalysis(
      @PathVariable Long cycleId,
      @RequestParam(defaultValue = "false") boolean force) {
    // CACHE-FIRST: Return immediately if cached (unless a forced refresh was requested)
    if (!force) {
      Optional<CycleRiskOverviewDTO> cached = asyncService.getCachedCycleRisk(cycleId);
      if (cached.isPresent()) {
        return ResponseEntity.ok(Map.of(
            "cached", true,
            "result", cached.get()
        ));
      }
    }

    // Cache miss (or forced refresh) - start async job
    String jobId = asyncService.startCycleRiskAnalysis(cycleId, force);
    return ResponseEntity.accepted().body(Map.of(
        "cached", false,
        "jobId", jobId,
        "status", "PENDING",
        "message", "AI analysis started. Poll /jobs/" + jobId + "/status for updates."
    ));
  }

  @PostMapping("/pitch/{pitchId}/ask")
  @Operation(summary = "Start async Q&A", 
      description = "Starts AI Q&A in background. Returns jobId for polling. (Q&A is not cached)")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<Map<String, String>> startQAQuestion(
      @PathVariable Long pitchId,
      @RequestBody RiskQuestionRequest request) {
    // Q&A is question-specific, typically not cached - go directly to async
    String jobId = asyncService.startQAQuestion(pitchId, request.getQuestion());
    return ResponseEntity.accepted().body(Map.of(
        "jobId", jobId,
        "status", "PENDING",
        "message", "AI Q&A started. Poll /jobs/" + jobId + "/status for updates."
    ));
  }

  // =========================================== 
  // JOB STATUS & RESULTS
  // ===========================================

  @GetMapping("/jobs/{jobId}/status")
  @Operation(summary = "Get job status", 
      description = "Check the status of an async AI job (PENDING, PROCESSING, COMPLETED, FAILED)")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
    return asyncService.getJobStatus(jobId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/jobs/{jobId}/pitch-risk")
  @Operation(summary = "Get pitch risk result", 
      description = "Retrieve completed pitch risk analysis result")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<PitchRiskDTO> getPitchRiskResult(@PathVariable String jobId) {
    return asyncService.getPitchRiskResult(jobId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/jobs/{jobId}/cycle-risk")
  @Operation(summary = "Get cycle risk result", 
      description = "Retrieve completed cycle risk analysis result")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<CycleRiskOverviewDTO> getCycleRiskResult(@PathVariable String jobId) {
    return asyncService.getCycleRiskResult(jobId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/jobs/{jobId}/qa")
  @Operation(summary = "Get Q&A result", 
      description = "Retrieve completed Q&A response")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'READ')")
  public ResponseEntity<RiskQuestionResponse> getQAResult(@PathVariable String jobId) {
    return asyncService.getQAResult(jobId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // ===========================================
  // JOB MANAGEMENT
  // ===========================================

  @DeleteMapping("/jobs/{jobId}")
  @Operation(summary = "Cancel job", 
      description = "Cancel a pending or processing job")
  @PreAuthorize("@permissionService.hasPermission('RISK', 'UPDATE')")
  public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
    boolean cancelled = asyncService.cancelJob(jobId);
    return ResponseEntity.ok(Map.of(
        "jobId", jobId,
        "cancelled", cancelled,
        "message", cancelled ? "Job cancelled" : "Job not found or already completed"
    ));
  }

  @GetMapping("/stats")
  @Operation(summary = "Get job statistics", 
      description = "Get current statistics for all async AI jobs")
  @PreAuthorize("@permissionService.hasPermission('ADMIN', 'READ')")
  public ResponseEntity<Map<String, Object>> getJobStats() {
    return ResponseEntity.ok(asyncService.getJobStats());
  }
}
