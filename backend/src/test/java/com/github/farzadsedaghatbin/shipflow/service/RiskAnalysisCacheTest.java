package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for Risk Analysis caching integration. Tests cover: - Pitch risk
 * caching with infinite TTL - Cache invalidation on data change - Cycle risk
 * caching - Change detection with hash comparison
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Risk Analysis Caching Tests")
class RiskAnalysisCacheTest {

  private AICacheConfig cacheConfig;
  private AICacheService cacheService;

  @BeforeEach
  void setUp() {
    cacheConfig = new AICacheConfig();
    cacheConfig.setEnabled(true);
    cacheConfig.setProvider("in-memory");

    AICacheConfig.RiskCacheConfig riskConfig = new AICacheConfig.RiskCacheConfig();
    riskConfig.setEnabled(true);
    riskConfig.setPitchTtlMinutes(0); // Infinite TTL
    riskConfig.setCycleTtlMinutes(0); // Infinite TTL
    riskConfig.setInvalidateOnDataChange(true);
    cacheConfig.setRisk(riskConfig);

    AICacheConfig.QACacheConfig qaConfig = new AICacheConfig.QACacheConfig();
    qaConfig.setEnabled(true);
    cacheConfig.setQa(qaConfig);

    cacheService = new AICacheService(cacheConfig);
    cacheService.init();
  }

  @Nested
  @DisplayName("Pitch Risk Cache Tests")
  class PitchRiskCacheTests {

    @Test
    @DisplayName("Should cache pitch risk with infinite TTL")
    void shouldCachePitchRiskWithInfiniteTTL() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO riskData = createTestPitchRiskDTO(pitchId, RiskLevel.HIGH, 75);
      String dataHash = "hash_v1";

      // When
      cacheService.cachePitchRisk(pitchId, true, riskData, dataHash);
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
      assertThat(result.get().getRiskScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should invalidate cache when data hash changes")
    void shouldInvalidateCacheWhenDataHashChanges() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO riskData = createTestPitchRiskDTO(pitchId, RiskLevel.MEDIUM, 50);

      // Cache with original hash
      cacheService.cachePitchRisk(pitchId, true, riskData, "hash_v1");

      // Update data version with different hash (simulating data change)
      cacheService.updatePitchDataVersion(pitchId, "hash_v2_different");

      // When
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then - Cache should be invalidated due to hash mismatch
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should keep cache when hash is unchanged")
    void shouldKeepCacheWhenHashUnchanged() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO riskData = createTestPitchRiskDTO(pitchId, RiskLevel.LOW, 25);
      String dataHash = "consistent_hash";

      // Cache with hash
      cacheService.cachePitchRisk(pitchId, true, riskData, dataHash);

      // When - Access multiple times
      for (int i = 0; i < 5; i++) {
        Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);
        assertThat(result).isPresent();
      }

      // Then - Verify hit count
      AICacheService.CacheStats stats = cacheService.getCacheStats();
      assertThat(stats.getTotalHits()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should separate AI and rule-based caches")
    void shouldSeparateAIAndRuleBasedCaches() {
      // Given
      Long pitchId = 1L;

      PitchRiskDTO aiRisk = createTestPitchRiskDTO(pitchId, RiskLevel.HIGH, 80);
      PitchRiskDTO ruleRisk = createTestPitchRiskDTO(pitchId, RiskLevel.MEDIUM, 50);

      // When - Use same hash since it's the same pitch data, just different analysis
      // modes
      String dataHash = "same_pitch_data";
      cacheService.cachePitchRisk(pitchId, true, aiRisk, dataHash);
      cacheService.cachePitchRisk(pitchId, false, ruleRisk, dataHash);

      Optional<PitchRiskDTO> aiResult = cacheService.getCachedPitchRisk(pitchId, true);
      Optional<PitchRiskDTO> ruleResult = cacheService.getCachedPitchRisk(pitchId, false);

      // Then
      assertThat(aiResult).isPresent();
      assertThat(aiResult.get().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
      assertThat(aiResult.get().getRiskScore()).isEqualTo(80);

      assertThat(ruleResult).isPresent();
      assertThat(ruleResult.get().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
      assertThat(ruleResult.get().getRiskScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should explicitly invalidate pitch cache")
    void shouldExplicitlyInvalidatePitchCache() {
      // Given - Use same hash for same pitch
      Long pitchId = 1L;
      String dataHash = "same_data_hash";
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId, RiskLevel.HIGH, 75), dataHash);
      cacheService.cachePitchRisk(pitchId, false, createTestPitchRiskDTO(pitchId, RiskLevel.MEDIUM, 50),
          dataHash);

      // Verify cached
      assertThat(cacheService.getCachedPitchRisk(pitchId, true)).isPresent();
      assertThat(cacheService.getCachedPitchRisk(pitchId, false)).isPresent();

      // When
      cacheService.invalidatePitchRiskCache(pitchId);

      // Then
      assertThat(cacheService.getCachedPitchRisk(pitchId, true)).isEmpty();
      assertThat(cacheService.getCachedPitchRisk(pitchId, false)).isEmpty();
    }

    @Test
    @DisplayName("Should not affect other pitches when invalidating")
    void shouldNotAffectOtherPitchesWhenInvalidating() {
      // Given
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L, RiskLevel.HIGH, 75), "hash1");
      cacheService.cachePitchRisk(2L, true, createTestPitchRiskDTO(2L, RiskLevel.LOW, 25), "hash2");
      cacheService.cachePitchRisk(3L, true, createTestPitchRiskDTO(3L, RiskLevel.MEDIUM, 50), "hash3");

      // When - Invalidate only pitch 2
      cacheService.invalidatePitchRiskCache(2L);

      // Then
      assertThat(cacheService.getCachedPitchRisk(1L, true)).isPresent();
      assertThat(cacheService.getCachedPitchRisk(2L, true)).isEmpty();
      assertThat(cacheService.getCachedPitchRisk(3L, true)).isPresent();
    }
  }

  @Nested
  @DisplayName("Cycle Risk Cache Tests")
  class CycleRiskCacheTests {

    @Test
    @DisplayName("Should cache cycle risk with infinite TTL")
    void shouldCacheCycleRiskWithInfiniteTTL() {
      // Given
      Long cycleId = 1L;
      CycleRiskOverviewDTO riskData = createTestCycleRiskDTO(cycleId, RiskLevel.MEDIUM, 3, 2, 1);

      // When
      cacheService.cacheCycleRisk(cycleId, true, riskData);
      Optional<CycleRiskOverviewDTO> result = cacheService.getCachedCycleRisk(cycleId, true);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getOverallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
      assertThat(result.get().getHighRiskPitches()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should invalidate cycle cache")
    void shouldInvalidateCycleCache() {
      // Given
      Long cycleId = 1L;
      cacheService.cacheCycleRisk(cycleId, true, createTestCycleRiskDTO(cycleId, RiskLevel.HIGH, 5, 3, 2));
      cacheService.cacheCycleRisk(cycleId, false, createTestCycleRiskDTO(cycleId, RiskLevel.MEDIUM, 3, 2, 1));

      // When
      cacheService.invalidateCycleRiskCache(cycleId);

      // Then
      assertThat(cacheService.getCachedCycleRisk(cycleId, true)).isEmpty();
      assertThat(cacheService.getCachedCycleRisk(cycleId, false)).isEmpty();
    }

    @Test
    @DisplayName("Should track hit count for cycle risk cache")
    void shouldTrackHitCountForCycleRiskCache() {
      // Given
      Long cycleId = 1L;
      cacheService.cacheCycleRisk(cycleId, true, createTestCycleRiskDTO(cycleId, RiskLevel.LOW, 1, 0, 0));

      // When - Multiple accesses
      cacheService.getCachedCycleRisk(cycleId, true);
      cacheService.getCachedCycleRisk(cycleId, true);
      cacheService.getCachedCycleRisk(cycleId, true);

      // Then
      AICacheService.CacheStats stats = cacheService.getCacheStats();
      assertThat(stats.getTotalHits()).isGreaterThanOrEqualTo(3);
    }
  }

  @Nested
  @DisplayName("Cache Configuration Tests")
  class CacheConfigurationTests {

    @Test
    @DisplayName("Should not cache when risk cache is disabled")
    void shouldNotCacheWhenRiskCacheDisabled() {
      // Given
      cacheConfig.getRisk().setEnabled(false);
      Long pitchId = 1L;

      // When
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId, RiskLevel.HIGH, 75), "hash");
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not cache when master cache is disabled")
    void shouldNotCacheWhenMasterCacheDisabled() {
      // Given
      cacheConfig.setEnabled(false);
      Long pitchId = 1L;

      // When
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId, RiskLevel.HIGH, 75), "hash");
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return correct stats with provider info")
    void shouldReturnCorrectStatsWithProviderInfo() {
      // Given
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L, RiskLevel.HIGH, 75), "h1");
      cacheService.cacheCycleRisk(1L, true, createTestCycleRiskDTO(1L, RiskLevel.MEDIUM, 2, 1, 0));

      // When
      AICacheService.CacheStats stats = cacheService.getCacheStats();

      // Then
      assertThat(stats.getProvider()).isEqualTo("in-memory");
      assertThat(stats.isRiskCacheEnabled()).isTrue();
      assertThat(stats.getTotalEntries()).isEqualTo(2);
      assertThat(stats.getPitchRiskEntries()).isEqualTo(1);
      assertThat(stats.getCycleRiskEntries()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Cache Cleanup Tests")
  class CacheCleanupTests {

    @Test
    @DisplayName("Should clear all caches")
    void shouldClearAllCaches() {
      // Given
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L, RiskLevel.HIGH, 75), "h1");
      cacheService.cachePitchRisk(2L, true, createTestPitchRiskDTO(2L, RiskLevel.LOW, 25), "h2");
      cacheService.cacheCycleRisk(1L, true, createTestCycleRiskDTO(1L, RiskLevel.MEDIUM, 2, 1, 0));

      assertThat(cacheService.getCacheStats().getTotalEntries()).isEqualTo(3);

      // When
      cacheService.clearAllCaches();

      // Then
      AICacheService.CacheStats stats = cacheService.getCacheStats();
      assertThat(stats.getTotalEntries()).isEqualTo(0);
      assertThat(stats.getPitchRiskEntries()).isEqualTo(0);
      assertThat(stats.getCycleRiskEntries()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not break during cleanup with no expired entries")
    void shouldNotBreakDuringCleanupWithNoExpiredEntries() {
      // Given - All entries have infinite TTL (no expiration)
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L, RiskLevel.HIGH, 75), "h1");

      // When
      cacheService.cleanupExpiredEntries();

      // Then - Entry should still be present (infinite TTL)
      assertThat(cacheService.getCachedPitchRisk(1L, true)).isPresent();
    }
  }

  // ===========================================
  // HELPER METHODS
  // ===========================================

  private PitchRiskDTO createTestPitchRiskDTO(Long pitchId, RiskLevel riskLevel, Integer riskScore) {
    PitchRiskDTO dto = new PitchRiskDTO();
    dto.setPitchId(pitchId);
    dto.setRiskLevel(riskLevel);
    dto.setRiskScore(riskScore);
    return dto;
  }

  private CycleRiskOverviewDTO createTestCycleRiskDTO(Long cycleId, RiskLevel overallRiskLevel, int highRisk,
      int mediumRisk, int lowRisk) {
    CycleRiskOverviewDTO dto = new CycleRiskOverviewDTO();
    dto.setCycleId(cycleId);
    dto.setOverallRiskLevel(overallRiskLevel);
    dto.setHighRiskPitches(highRisk);
    dto.setMediumRiskPitches(mediumRisk);
    dto.setLowRiskPitches(lowRisk);
    return dto;
  }
}
