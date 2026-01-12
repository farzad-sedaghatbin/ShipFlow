package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.metrics.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CustomMetricService;
import com.github.farzadsedaghatbin.shipflow.service.MetricFormulaParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for custom metrics management
 */
@RestController
@RequestMapping("/api/metrics/custom")
@RequiredArgsConstructor
@Slf4j
public class CustomMetricController {

    private final CustomMetricService customMetricService;
    private final UserRepository userRepository;

    /**
     * Create a new custom metric
     */
    @PostMapping
    public ResponseEntity<CustomMetricDTO> createMetric(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCustomMetricRequest request) {
        Long userId = getUserId(userDetails);
        log.info("Creating custom metric for user: {}", userId);
        CustomMetricDTO metric = customMetricService.createMetric(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(metric);
    }

    /**
     * Get all custom metrics for the current user
     */
    @GetMapping
    public ResponseEntity<List<CustomMetricDTO>> getUserMetrics(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        log.info("Fetching custom metrics for user: {}", userId);
        List<CustomMetricDTO> metrics = customMetricService.getUserMetrics(userId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get a specific metric by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomMetricDTO> getMetric(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        log.info("Fetching metric {} for user: {}", id, userId);
        CustomMetricDTO metric = customMetricService.getMetric(userId, id);
        return ResponseEntity.ok(metric);
    }

    /**
     * Update an existing custom metric
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomMetricDTO> updateMetric(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomMetricRequest request) {
        Long userId = getUserId(userDetails);
        log.info("Updating metric {} for user: {}", id, userId);
        CustomMetricDTO metric = customMetricService.updateMetric(userId, id, request);
        return ResponseEntity.ok(metric);
    }

    /**
     * Delete a custom metric
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetric(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        log.info("Deleting metric {} for user: {}", id, userId);
        customMetricService.deleteMetric(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Calculate the current value of a metric
     */
    @PostMapping("/{id}/calculate")
    public ResponseEntity<MetricValueDTO> calculateMetricValue(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        log.info("Calculating value for metric {} (user: {})", id, userId);
        MetricValueDTO value = customMetricService.calculateMetricValue(userId, id);
        return ResponseEntity.ok(value);
    }

    /**
     * Get historical values for a metric
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<MetricValueDTO>> getMetricHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int limit) {
        Long userId = getUserId(userDetails);
        log.info("Fetching history for metric {} (user: {}, limit: {})", id, userId, limit);
        List<MetricValueDTO> history = customMetricService.getMetricHistory(userId, id, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * Validate a formula without saving
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFormula(
            @RequestBody Map<String, String> request) {
        String formula = request.get("formula");
        log.info("Validating formula");

        MetricFormulaParser.ValidationResult result = customMetricService.validateFormula(formula);

        return ResponseEntity.ok(Map.of(
                "valid", result.isValid(),
                "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        log.error("Security error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
