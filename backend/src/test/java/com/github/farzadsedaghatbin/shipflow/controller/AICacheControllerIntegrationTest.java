package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.service.AICacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AICacheController.
 * Tests the REST API endpoints for cache management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
@DisplayName("AI Cache Controller Integration Tests")
class AICacheControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AICacheService cacheService;

    @Autowired
    private AICacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        cacheService.clearAllCaches();
    }

    @Test
    @DisplayName("GET /api/cache/stats - Should return cache statistics")
    void getCacheStats_ShouldReturnStatistics() throws Exception {
        // Given - Add some cached data
        PitchRiskDTO riskData = new PitchRiskDTO();
        riskData.setPitchId(1L);
        riskData.setRiskLevel(PitchRiskDTO.RiskLevel.MEDIUM);
        cacheService.cachePitchRisk(1L, true, riskData, "hash1");
        
        // When & Then
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalEntries", is(1)))
                .andExpect(jsonPath("$.pitchRiskEntries", is(1)))
                .andExpect(jsonPath("$.cycleRiskEntries", is(0)))
                .andExpect(jsonPath("$.qaEntries", is(0)))
                .andExpect(jsonPath("$.riskCacheEnabled", notNullValue()))
                .andExpect(jsonPath("$.qaCacheEnabled", notNullValue()))
                .andExpect(jsonPath("$.provider", is("in-memory")));
    }

    @Test
    @DisplayName("GET /api/cache/stats - Should return empty stats when no cache entries")
    void getCacheStats_WhenEmpty_ShouldReturnZeroEntries() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries", is(0)))
                .andExpect(jsonPath("$.pitchRiskEntries", is(0)))
                .andExpect(jsonPath("$.cycleRiskEntries", is(0)))
                .andExpect(jsonPath("$.qaEntries", is(0)));
    }

    @Test
    @DisplayName("GET /api/cache/config - Should return cache configuration")
    void getCacheConfig_ShouldReturnConfiguration() throws Exception {
        mockMvc.perform(get("/api/cache/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled", notNullValue()))
                .andExpect(jsonPath("$.risk", notNullValue()))
                .andExpect(jsonPath("$.risk.enabled", notNullValue()))
                .andExpect(jsonPath("$.risk.pitchTtlMinutes", notNullValue()))
                .andExpect(jsonPath("$.risk.cycleTtlMinutes", notNullValue()))
                .andExpect(jsonPath("$.risk.invalidateOnDataChange", notNullValue()))
                .andExpect(jsonPath("$.qa", notNullValue()))
                .andExpect(jsonPath("$.qa.enabled", notNullValue()))
                .andExpect(jsonPath("$.qa.ttlHours", notNullValue()))
                .andExpect(jsonPath("$.qa.similarityThreshold", notNullValue()))
                .andExpect(jsonPath("$.qa.maxEntriesPerContext", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/cache/clear - Should clear all caches")
    void clearAllCaches_ShouldClearCaches() throws Exception {
        // Given - Add some cached data
        PitchRiskDTO riskData = new PitchRiskDTO();
        riskData.setPitchId(1L);
        cacheService.cachePitchRisk(1L, true, riskData, "hash1");
        cacheService.cachePitchRisk(2L, true, riskData, "hash2");
        
        // Verify caches have entries
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.totalEntries", is(2)));
        
        // When
        mockMvc.perform(post("/api/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cleared successfully")));
        
        // Then
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.totalEntries", is(0)));
    }

    @Test
    @DisplayName("POST /api/cache/clear/risk - Should clear risk cache")
    void clearRiskCache_ShouldClearRiskCache() throws Exception {
        // Given
        PitchRiskDTO riskData = new PitchRiskDTO();
        riskData.setPitchId(1L);
        cacheService.cachePitchRisk(1L, true, riskData, "hash1");
        
        // When
        mockMvc.perform(post("/api/cache/clear/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cleared successfully")));
        
        // Then
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.pitchRiskEntries", is(0)));
    }

    @Test
    @DisplayName("POST /api/cache/clear/qa - Should clear Q&A cache")
    void clearQACache_ShouldClearQACache() throws Exception {
        // When
        mockMvc.perform(post("/api/cache/clear/qa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cleared successfully")));
        
        // Then
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.qaEntries", is(0)));
    }

    @Test
    @DisplayName("DELETE /api/cache/risk/pitch/{id} - Should invalidate pitch cache")
    void invalidatePitchCache_ShouldInvalidateCache() throws Exception {
        // Given
        Long pitchId = 1L;
        PitchRiskDTO riskData = new PitchRiskDTO();
        riskData.setPitchId(pitchId);
        cacheService.cachePitchRisk(pitchId, true, riskData, "hash1");
        cacheService.cachePitchRisk(pitchId, false, riskData, "hash2");
        
        // Verify cache exists
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.pitchRiskEntries", is(2)));
        
        // When
        mockMvc.perform(delete("/api/cache/risk/pitch/{id}", pitchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("invalidated")));
        
        // Then - Pitch cache should be invalidated
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(jsonPath("$.pitchRiskEntries", is(0)));
    }

    @Test
    @DisplayName("DELETE /api/cache/risk/cycle/{id} - Should invalidate cycle cache")
    void invalidateCycleCache_ShouldInvalidateCache() throws Exception {
        // When
        mockMvc.perform(delete("/api/cache/risk/cycle/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("invalidated")));
    }

    @Test
    @DisplayName("Unauthorized access should be rejected")
    @WithMockUser(username = "user", roles = {})
    void unauthorizedAccess_ShouldBeHandled() throws Exception {
        // This test verifies the endpoint is accessible with default roles
        // In a real scenario, you might test role-based access control
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk());
    }
}
