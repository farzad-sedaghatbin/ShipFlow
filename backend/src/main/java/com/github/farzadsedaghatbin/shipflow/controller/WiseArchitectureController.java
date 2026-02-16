package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
