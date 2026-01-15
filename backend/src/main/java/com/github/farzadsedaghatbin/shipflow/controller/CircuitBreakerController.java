package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CircuitBreakerDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.service.CircuitBreakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Circuit Breaker Controller - Shape Up safety valve
 * Manages overflow detection and pitch killing
 */
@RestController
@RequestMapping("/api/circuit-breaker")
@RequiredArgsConstructor
@Tag(name = "Circuit Breaker", description = "Shape Up safety valve - detect and manage overflowing pitches")
public class CircuitBreakerController {

    private final CircuitBreakerService circuitBreakerService;

    @GetMapping("/cycle/{cycleId}/overflow")
    @Operation(summary = "Detect overflowing pitches", 
               description = "Find pitches that exceed their time budget. Default threshold is 100% (fully consumed appetite)")
    public ResponseEntity<List<CircuitBreakerDTO>> detectOverflow(
            @PathVariable Long cycleId,
            @RequestParam(defaultValue = "100.0") Double threshold) {
        return ResponseEntity.ok(circuitBreakerService.detectOverflowPitches(cycleId, threshold));
    }

    @GetMapping("/cycle/{cycleId}/triggered")
    @Operation(summary = "Get triggered circuit breakers",
               description = "Get all pitches with active circuit breakers in a cycle")
    public ResponseEntity<List<CircuitBreakerDTO>> getTriggered(@PathVariable Long cycleId) {
        return ResponseEntity.ok(circuitBreakerService.getTriggeredCircuitBreakers(cycleId));
    }

    @PostMapping("/pitch/{pitchId}/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Trigger circuit breaker",
               description = "Manually flag a pitch as overflowing its time budget")
    public ResponseEntity<CircuitBreakerDTO> trigger(
            @PathVariable Long pitchId,
            @RequestParam String reason) {
        return ResponseEntity.ok(circuitBreakerService.triggerCircuitBreaker(pitchId, reason));
    }

    @PostMapping("/pitch/{pitchId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Resolve circuit breaker",
               description = "Clear circuit breaker flag and set new status")
    public ResponseEntity<CircuitBreakerDTO> resolve(
            @PathVariable Long pitchId,
            @RequestParam PitchStatus newStatus) {
        return ResponseEntity.ok(circuitBreakerService.resolveCircuitBreaker(pitchId, newStatus));
    }

    @PostMapping("/pitch/{pitchId}/kill")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Kill pitch",
               description = "Permanently stop work on pitch due to overflow (Shape Up safety valve)")
    public ResponseEntity<CircuitBreakerDTO> kill(
            @PathVariable Long pitchId,
            @RequestParam String reason) {
        return ResponseEntity.ok(circuitBreakerService.killPitch(pitchId, reason));
    }
}
