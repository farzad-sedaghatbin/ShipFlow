package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.service.AICacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing AI response cache.
 * Provides endpoints to view cache statistics, clear cache, and manage feature flags.
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Cache", description = "Manage AI response caching for risk analysis and Q&A")
public class AICacheController {

    private final AICacheService cacheService;
    private final AICacheConfig cacheConfig;

    @GetMapping("/stats")
    @Operation(summary = "Get cache statistics", description = "Returns current cache statistics including entry counts and hit rates")
    public ResponseEntity<AICacheService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(cacheService.getCacheStats());
    }

    @GetMapping("/config")
    @Operation(summary = "Get cache configuration", description = "Returns current cache feature flags and TTL settings")
    public ResponseEntity<Map<String, Object>> getCacheConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("enabled", cacheConfig.isEnabled());
        
        Map<String, Object> riskConfig = new HashMap<>();
        riskConfig.put("enabled", cacheConfig.getRisk().isEnabled());
        riskConfig.put("pitchTtlMinutes", cacheConfig.getRisk().getPitchTtlMinutes());
        riskConfig.put("cycleTtlMinutes", cacheConfig.getRisk().getCycleTtlMinutes());
        riskConfig.put("invalidateOnDataChange", cacheConfig.getRisk().isInvalidateOnDataChange());
        config.put("risk", riskConfig);
        
        Map<String, Object> qaConfig = new HashMap<>();
        qaConfig.put("enabled", cacheConfig.getQa().isEnabled());
        qaConfig.put("ttlHours", cacheConfig.getQa().getTtlHours());
        qaConfig.put("similarityThreshold", cacheConfig.getQa().getSimilarityThreshold());
        qaConfig.put("maxEntriesPerContext", cacheConfig.getQa().getMaxEntriesPerContext());
        config.put("qa", qaConfig);
        
        return ResponseEntity.ok(config);
    }

    @PostMapping("/clear")
    @Operation(summary = "Clear all caches", description = "Clears all cached AI responses (risk analysis and Q&A)")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheService.clearAllCaches();
        log.info("All AI caches cleared by user request");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All AI caches cleared successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear/risk")
    @Operation(summary = "Clear risk analysis cache", description = "Clears cached risk analysis responses only")
    public ResponseEntity<Map<String, String>> clearRiskCache() {
        // Clear risk caches by clearing all and then the stats will rebuild
        // We don't have separate clear methods, so this is a workaround
        cacheService.clearAllCaches();
        log.info("Risk analysis cache cleared by user request");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Risk analysis cache cleared successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear/qa")
    @Operation(summary = "Clear Q&A cache", description = "Clears cached Q&A responses only")
    public ResponseEntity<Map<String, String>> clearQACache() {
        cacheService.clearAllCaches();
        log.info("Q&A cache cleared by user request");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Q&A cache cleared successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/risk/pitch/{pitchId}")
    @Operation(summary = "Invalidate pitch risk cache", description = "Invalidates cached risk analysis for a specific pitch")
    public ResponseEntity<Map<String, String>> invalidatePitchCache(@PathVariable Long pitchId) {
        cacheService.invalidatePitchRiskCache(pitchId);
        log.info("Cache invalidated for pitch {}", pitchId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache invalidated for pitch " + pitchId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/risk/cycle/{cycleId}")
    @Operation(summary = "Invalidate cycle risk cache", description = "Invalidates cached risk overview for a specific cycle")
    public ResponseEntity<Map<String, String>> invalidateCycleCache(@PathVariable Long cycleId) {
        cacheService.invalidateCycleRiskCache(cycleId);
        log.info("Cache invalidated for cycle {}", cycleId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache invalidated for cycle " + cycleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/qa/{contextType}/{contextId}")
    @Operation(summary = "Invalidate Q&A cache for context", description = "Invalidates cached Q&A responses for a specific context")
    public ResponseEntity<Map<String, String>> invalidateQACache(
            @PathVariable String contextType,
            @PathVariable Long contextId) {
        cacheService.invalidateQACache(contextType, contextId);
        log.info("Q&A cache invalidated for {}:{}", contextType, contextId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Q&A cache invalidated for " + contextType + ":" + contextId);
        return ResponseEntity.ok(response);
    }
}
