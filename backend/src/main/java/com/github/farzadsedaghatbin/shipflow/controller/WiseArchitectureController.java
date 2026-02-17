package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.AsyncWiseArchitectureService;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureHistoryService;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureJob;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for the Wise Architecture feature.
 * Provides endpoints for stack detection, solution generation, and follow-up conversations.
 */
@RestController
@RequestMapping("/api/wise-architecture")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wise Architecture", description = "AI-powered technical solution generator (Experimental)")
public class WiseArchitectureController {

    private final WiseArchitectureService wiseArchitectureService;
    private final AsyncWiseArchitectureService asyncService;
    private final WiseArchitectureHistoryService historyService;
    private final UserRepository userRepository;

    @PostMapping("/detect-stacks")
    @Operation(summary = "Detect technology stacks in repositories",
        description = "Scans the selected repositories to identify technology stacks (Mobile, Backend, Web)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stacks detected successfully"),
        @ApiResponse(responseCode = "403", description = "Feature is not enabled"),
        @ApiResponse(responseCode = "404", description = "Pitch or repositories not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<DetectStacksResponseDTO> detectStacks(
            @Valid @RequestBody DetectStacksRequestDTO request) {
        
        log.info("Detecting stacks for pitch {} in {} repositories", 
            request.getPitchId(), request.getRepositoryIds().size());
        
        DetectStacksResponseDTO response = wiseArchitectureService.detectStacks(request);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    @Operation(summary = "Generate technical solution document",
        description = "Generates a comprehensive technical solution for the selected pitch and technology stacks, " +
            "including architecture overview, reusable services, recommended libraries, and implementation steps")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solution generated successfully"),
        @ApiResponse(responseCode = "403", description = "Feature is not enabled"),
        @ApiResponse(responseCode = "404", description = "Pitch or repositories not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<WiseArchitectureResponseDTO> analyze(
            @Valid @RequestBody WiseArchitectureRequestDTO request) {
        
        log.info("Generating solution for pitch {} with stacks: {}", 
            request.getPitchId(), request.getSelectedStacks());
        
        WiseArchitectureResponseDTO response = wiseArchitectureService.analyze(request);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/follow-up")
    @Operation(summary = "Ask a follow-up question about a generated solution",
        description = "Submit a follow-up question about a previously generated solution. " +
            "If asking for code, will return a ready-to-use prompt for Copilot or other AI assistants")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Response generated successfully"),
        @ApiResponse(responseCode = "403", description = "Feature is not enabled"),
        @ApiResponse(responseCode = "404", description = "Session not found or expired")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<FollowUpResponseDTO> followUp(
            @Valid @RequestBody FollowUpQuestionDTO request) {
        
        log.info("Processing follow-up question for session {}", request.getSessionId());
        
        FollowUpResponseDTO response = wiseArchitectureService.handleFollowUp(request);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if Wise Architecture feature is enabled",
        description = "Returns the current status of the Wise Architecture feature")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER', 'VIEWER')")
    public ResponseEntity<FeatureStatusDTO> getFeatureStatus() {
        try {
            wiseArchitectureService.checkFeatureEnabled();
            return ResponseEntity.ok(FeatureStatusDTO.builder()
                .enabled(true)
                .message("Wise Architecture feature is enabled")
                .build());
        } catch (com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException e) {
            return ResponseEntity.ok(FeatureStatusDTO.builder()
                .enabled(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * Simple DTO for feature status response.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class FeatureStatusDTO {
        private Boolean enabled;
        private String message;
    }

    // =====================================================================
    // ASYNC ENDPOINTS - For large repositories that may timeout
    // =====================================================================

    @PostMapping("/async/detect-stacks")
    @Operation(summary = "Start async stack detection",
        description = "Starts an asynchronous stack detection job. Returns immediately with a job ID " +
            "that can be used to poll for status and results. Use this for large repositories.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and started"),
        @ApiResponse(responseCode = "403", description = "Feature is not enabled"),
        @ApiResponse(responseCode = "409", description = "Duplicate request - job already in progress")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<JobStartResponseDTO> startDetectStacksAsync(
            @Valid @RequestBody DetectStacksRequestDTO request) {
        
        log.info("Starting async stack detection for pitch {} in {} repositories", 
            request.getPitchId(), request.getRepositoryIds().size());
        
        // Check feature enabled first
        wiseArchitectureService.checkFeatureEnabled();
        
        String jobId = asyncService.startDetectStacks(request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(JobStartResponseDTO.builder()
                .jobId(jobId)
                .status("PENDING")
                .message("Stack detection job started")
                .build());
    }

    @PostMapping("/async/analyze")
    @Operation(summary = "Start async solution generation",
        description = "Starts an asynchronous solution generation job. Returns immediately with a job ID " +
            "that can be used to poll for status and results. Use this for complex analyses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and started"),
        @ApiResponse(responseCode = "403", description = "Feature is not enabled"),
        @ApiResponse(responseCode = "409", description = "Duplicate request - job already in progress")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<JobStartResponseDTO> startAnalyzeAsync(
            @Valid @RequestBody WiseArchitectureRequestDTO request) {
        
        log.info("Starting async solution generation for pitch {} with stacks: {}", 
            request.getPitchId(), request.getSelectedStacks());
        
        // Check feature enabled first
        wiseArchitectureService.checkFeatureEnabled();
        
        String jobId = asyncService.startAnalyze(request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(JobStartResponseDTO.builder()
                .jobId(jobId)
                .status("PENDING")
                .message("Solution generation job started")
                .build());
    }

    @GetMapping("/jobs/{jobId}/status")
    @Operation(summary = "Get job status",
        description = "Returns the current status and progress of a job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job status returned"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<JobStatusResponseDTO> getJobStatus(@PathVariable String jobId) {
        Optional<AsyncWiseArchitectureService.JobStatusResponse> jobOpt = asyncService.getJobStatus(jobId);
        
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AsyncWiseArchitectureService.JobStatusResponse job = jobOpt.get();
        return ResponseEntity.ok(JobStatusResponseDTO.builder()
            .jobId(job.getJobId())
            .jobType(job.getJobType())
            .status(job.getStatus().name())
            .progress(job.getProgress())
            .progressMessage(job.getProgressMessage())
            .createdAt(job.getCreatedAt())
            .completedAt(job.getCompletedAt())
            .errorMessage(job.getErrorMessage())
            .build());
    }

    @GetMapping("/jobs/{jobId}/result")
    @Operation(summary = "Get job result",
        description = "Returns the result of a completed job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job result returned"),
        @ApiResponse(responseCode = "202", description = "Job still in progress"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "500", description = "Job failed")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<?> getJobResult(@PathVariable String jobId) {
        Optional<AsyncWiseArchitectureService.JobStatusResponse> statusOpt = asyncService.getJobStatus(jobId);
        
        if (statusOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AsyncWiseArchitectureService.JobStatusResponse status = statusOpt.get();
        
        switch (status.getStatus()) {
            case COMPLETED:
                // Get the actual result based on job type
                if ("detect_stacks".equals(status.getJobType())) {
                    return asyncService.getDetectStacksResult(jobId)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
                } else {
                    return asyncService.getAnalyzeResult(jobId)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
                }
            case FAILED:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "jobId", status.getJobId(),
                        "status", "FAILED",
                        "error", status.getErrorMessage() != null ? status.getErrorMessage() : "Unknown error"
                    ));
            case CANCELLED:
                return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
                        "jobId", status.getJobId(),
                        "status", "CANCELLED",
                        "message", "Job was cancelled"
                    ));
            default: // PENDING or PROCESSING
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(JobStatusResponseDTO.builder()
                        .jobId(status.getJobId())
                        .jobType(status.getJobType())
                        .status(status.getStatus().name())
                        .progress(status.getProgress())
                        .progressMessage(status.getProgressMessage())
                        .build());
        }
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Cancel a job",
        description = "Attempts to cancel a running job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "409", description = "Job cannot be cancelled (already completed/failed)")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable String jobId) {
        boolean cancelled = asyncService.cancelJob(jobId);
        
        if (cancelled) {
            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", "CANCELLED",
                "message", "Job cancelled successfully"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "jobId", jobId,
                    "message", "Job cannot be cancelled (not found or already completed)"
                ));
        }
    }

    // =====================================================================
    // ADVICE HISTORY ENDPOINTS
    // =====================================================================

    @GetMapping("/history")
    @Operation(summary = "Get advice history for current user",
        description = "Returns a paginated list of conversation summaries for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History returned successfully")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<org.springframework.data.domain.Page<AdviceHistoryDTO.ConversationSummary>> getMyHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getUserId(userDetails);
        log.info("Fetching advice history for user {}", userId);
        
        var conversations = historyService.getUserConversations(userId, page, size);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/history/pitch/{pitchId}")
    @Operation(summary = "Get advice history for a pitch",
        description = "Returns all conversation summaries for a specific pitch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History returned successfully"),
        @ApiResponse(responseCode = "404", description = "Pitch not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<java.util.List<AdviceHistoryDTO.ConversationSummary>> getPitchHistory(
            @PathVariable Long pitchId) {
        
        log.info("Fetching advice history for pitch {}", pitchId);
        
        var conversations = historyService.getPitchConversations(pitchId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/history/conversation/{conversationId}")
    @Operation(summary = "Get full conversation thread",
        description = "Returns all messages in a conversation, ordered chronologically")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation returned successfully"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<java.util.List<AdviceHistoryDTO>> getConversation(
            @PathVariable String conversationId) {
        
        log.info("Fetching conversation {}", conversationId);
        
        var messages = historyService.getConversation(conversationId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/history/{adviceId}")
    @Operation(summary = "Get a single advice entry",
        description = "Returns a specific advice entry by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advice returned successfully"),
        @ApiResponse(responseCode = "404", description = "Advice not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<AdviceHistoryDTO> getAdvice(@PathVariable Long adviceId) {
        
        log.info("Fetching advice {}", adviceId);
        
        var advice = historyService.getAdvice(adviceId);
        return ResponseEntity.ok(advice);
    }

    @PostMapping("/history/{adviceId}/feedback")
    @Operation(summary = "Submit feedback for advice",
        description = "Submit feedback (helpful/not helpful) for a specific advice entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feedback submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot submit feedback for another user's advice"),
        @ApiResponse(responseCode = "404", description = "Advice not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DEVELOPER')")
    public ResponseEntity<AdviceHistoryDTO> submitFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long adviceId,
            @RequestBody AdviceHistoryDTO.FeedbackRequest feedback) {
        
        Long userId = getUserId(userDetails);
        log.info("User {} submitting feedback for advice {}", userId, adviceId);
        
        var advice = historyService.submitFeedback(
            adviceId, 
            userId,
            feedback.getHelpful(),
            feedback.getFeedbackText());
        return ResponseEntity.ok(advice);
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()))
            .getId();
    }

    // =====================================================================
    // ASYNC JOB DTOs
    // =====================================================================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class JobStartResponseDTO {
        private String jobId;
        private String status;
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class JobStatusResponseDTO {
        private String jobId;
        private String jobType;
        private String status;
        private Integer progress;
        private String progressMessage;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime completedAt;
        private String errorMessage;
    }
}
