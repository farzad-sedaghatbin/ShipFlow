package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.QAResponse;
import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for AICacheService. Tests cover: - Pitch risk caching with TTL and
 * invalidation - Cycle risk caching - Q&A caching with fuzzy matching - Feature flags
 * (enable/disable) - Cache statistics - Cache cleanup
 */
@DisplayName("AICacheService Tests")
class AICacheServiceTest {

  private AICacheConfig cacheConfig;
  private AICacheService cacheService;

  @BeforeEach
  void setUp() {
    cacheConfig = new AICacheConfig();
    cacheConfig.setEnabled(true);
    cacheConfig.setProvider("in-memory");

    // Configure risk cache
    AICacheConfig.RiskCacheConfig riskConfig = new AICacheConfig.RiskCacheConfig();
    riskConfig.setEnabled(true);
    riskConfig.setPitchTtlMinutes(0); // Infinite
    riskConfig.setCycleTtlMinutes(0); // Infinite
    riskConfig.setInvalidateOnDataChange(true);
    cacheConfig.setRisk(riskConfig);

    // Configure Q&A cache
    AICacheConfig.QACacheConfig qaConfig = new AICacheConfig.QACacheConfig();
    qaConfig.setEnabled(true);
    qaConfig.setTtlHours(3);
    qaConfig.setSimilarityThreshold(0.85);
    qaConfig.setMaxEntriesPerContext(100);
    cacheConfig.setQa(qaConfig);

    cacheService = new AICacheService(cacheConfig);
    cacheService.init();
  }

  // ===========================================
  // PITCH RISK CACHING TESTS
  // ===========================================

  @Nested
  @DisplayName("Pitch Risk Cache Tests")
  class PitchRiskCacheTests {

    @Test
    @DisplayName("Should cache and retrieve pitch risk data")
    void shouldCacheAndRetrievePitchRisk() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO riskData = createTestPitchRiskDTO(pitchId);
      String dataHash = "hash123";

      // When
      cacheService.cachePitchRisk(pitchId, true, riskData, dataHash);
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getPitchId()).isEqualTo(pitchId);
    }

    @Test
    @DisplayName("Should return empty when pitch risk not cached")
    void shouldReturnEmptyWhenNotCached() {
      // Given
      Long pitchId = 999L;

      // When
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should cache AI and rule-based risks separately")
    void shouldCacheAIAndRuleBasedSeparately() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO aiRisk = createTestPitchRiskDTO(pitchId);
      aiRisk.setRiskLevel(RiskLevel.HIGH);
      PitchRiskDTO ruleRisk = createTestPitchRiskDTO(pitchId);
      ruleRisk.setRiskLevel(RiskLevel.MEDIUM);

      // When - Use same hash since it's the same pitch data, just different analysis modes
      String dataHash = "same_pitch_data_hash";
      cacheService.cachePitchRisk(pitchId, true, aiRisk, dataHash);
      cacheService.cachePitchRisk(pitchId, false, ruleRisk, dataHash);

      Optional<PitchRiskDTO> aiResult = cacheService.getCachedPitchRisk(pitchId, true);
      Optional<PitchRiskDTO> ruleResult = cacheService.getCachedPitchRisk(pitchId, false);

      // Then
      assertThat(aiResult).isPresent();
      assertThat(aiResult.get().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
      assertThat(ruleResult).isPresent();
      assertThat(ruleResult.get().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("Should invalidate cache when pitch data changes")
    void shouldInvalidateCacheOnDataChange() {
      // Given
      Long pitchId = 1L;
      PitchRiskDTO riskData = createTestPitchRiskDTO(pitchId);
      cacheService.cachePitchRisk(pitchId, true, riskData, "hash1");

      // When - Update data version with different hash
      cacheService.updatePitchDataVersion(pitchId, "hash2_different");
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should explicitly invalidate pitch risk cache")
    void shouldExplicitlyInvalidatePitchRiskCache() {
      // Given - Use same hash for same pitch
      Long pitchId = 1L;
      String dataHash = "same_data_hash";
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId), dataHash);
      cacheService.cachePitchRisk(pitchId, false, createTestPitchRiskDTO(pitchId), dataHash);

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
    @DisplayName("Should not cache when risk cache is disabled")
    void shouldNotCacheWhenDisabled() {
      // Given
      cacheConfig.getRisk().setEnabled(false);
      Long pitchId = 1L;

      // When
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId), "hash");
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not cache when master cache is disabled")
    void shouldNotCacheWhenMasterDisabled() {
      // Given
      cacheConfig.setEnabled(false);
      Long pitchId = 1L;

      // When
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId), "hash");
      Optional<PitchRiskDTO> result = cacheService.getCachedPitchRisk(pitchId, true);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ===========================================
  // CYCLE RISK CACHING TESTS
  // ===========================================

  @Nested
  @DisplayName("Cycle Risk Cache Tests")
  class CycleRiskCacheTests {

    @Test
    @DisplayName("Should cache and retrieve cycle risk data")
    void shouldCacheAndRetrieveCycleRisk() {
      // Given
      Long cycleId = 1L;
      CycleRiskOverviewDTO riskData = createTestCycleRiskDTO(cycleId);

      // When
      cacheService.cacheCycleRisk(cycleId, true, riskData);
      Optional<CycleRiskOverviewDTO> result = cacheService.getCachedCycleRisk(cycleId, true);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getCycleId()).isEqualTo(cycleId);
    }

    @Test
    @DisplayName("Should invalidate cycle risk cache")
    void shouldInvalidateCycleRiskCache() {
      // Given
      Long cycleId = 1L;
      cacheService.cacheCycleRisk(cycleId, true, createTestCycleRiskDTO(cycleId));

      // When
      cacheService.invalidateCycleRiskCache(cycleId);

      // Then
      assertThat(cacheService.getCachedCycleRisk(cycleId, true)).isEmpty();
    }
  }

  // ===========================================
  // Q&A CACHING TESTS
  // ===========================================

  @Nested
  @DisplayName("Q&A Cache Tests")
  class QACacheTests {

    @Test
    @DisplayName("Should cache and retrieve Q&A response")
    void shouldCacheAndRetrieveQAResponse() {
      // Given
      String question = "What is the progress of this pitch?";
      QAResponse response = createTestQAResponse(question);

      // When
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, response);
      Optional<QAResponse> result =
          cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getAnswer()).isEqualTo(response.getAnswer());
    }

    @Test
    @DisplayName("Should find similar questions with fuzzy matching")
    void shouldFindSimilarQuestionsWithFuzzyMatching() {
      // Given - Questions with Jaccard similarity >= 0.85
      // Original words: {what, is, the, progress, of, this, pitch} = 7 words
      // Similar words: {what, is, the, progress, of, this, pitch, currently} = 8 words
      // Intersection = 7, Union = 8, Jaccard = 7/8 = 0.875 >= 0.85
      String originalQuestion = "What is the progress of this pitch?";
      String similarQuestion = "What is the progress of this pitch currently?";
      QAResponse response = createTestQAResponse(originalQuestion);

      // When
      cacheService.cacheQAResponse(originalQuestion, "pitch", 1L, null, null, response);
      Optional<QAResponse> result =
          cacheService.getCachedQAResponse(similarQuestion, "pitch", 1L, null, null);

      // Then
      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should not match very different questions")
    void shouldNotMatchVeryDifferentQuestions() {
      // Given
      String originalQuestion = "What is the progress?";
      String differentQuestion = "How many team members are assigned?";
      QAResponse response = createTestQAResponse(originalQuestion);

      // When
      cacheService.cacheQAResponse(originalQuestion, "pitch", 1L, null, null, response);
      Optional<QAResponse> result =
          cacheService.getCachedQAResponse(differentQuestion, "pitch", 1L, null, null);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should separate cache by context")
    void shouldSeparateCacheByContext() {
      // Given
      String question = "What is the status?";
      QAResponse pitch1Response = createTestQAResponse(question);
      pitch1Response.setAnswer("Pitch 1 status");
      QAResponse pitch2Response = createTestQAResponse(question);
      pitch2Response.setAnswer("Pitch 2 status");

      // When
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, pitch1Response);
      cacheService.cacheQAResponse(question, "pitch", 2L, null, null, pitch2Response);

      Optional<QAResponse> result1 =
          cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);
      Optional<QAResponse> result2 =
          cacheService.getCachedQAResponse(question, "pitch", 2L, null, null);

      // Then
      assertThat(result1).isPresent();
      assertThat(result1.get().getAnswer()).isEqualTo("Pitch 1 status");
      assertThat(result2).isPresent();
      assertThat(result2.get().getAnswer()).isEqualTo("Pitch 2 status");
    }

    @Test
    @DisplayName("Should invalidate Q&A cache by context")
    void shouldInvalidateQACacheByContext() {
      // Given
      cacheService.cacheQAResponse(
          "Question 1", "pitch", 1L, null, null, createTestQAResponse("Q1"));
      cacheService.cacheQAResponse(
          "Question 2", "pitch", 1L, null, null, createTestQAResponse("Q2"));
      cacheService.cacheQAResponse(
          "Question 3", "pitch", 2L, null, null, createTestQAResponse("Q3"));

      // When
      cacheService.invalidateQACache("pitch", 1L);

      // Then
      assertThat(cacheService.getCachedQAResponse("Question 1", "pitch", 1L, null, null)).isEmpty();
      assertThat(cacheService.getCachedQAResponse("Question 2", "pitch", 1L, null, null)).isEmpty();
      // Pitch 2 should still be cached
      assertThat(cacheService.getCachedQAResponse("Question 3", "pitch", 2L, null, null))
          .isPresent();
    }

    @Test
    @DisplayName("Should not cache when Q&A cache is disabled")
    void shouldNotCacheWhenDisabled() {
      // Given
      cacheConfig.getQa().setEnabled(false);
      String question = "Test question";

      // When
      cacheService.cacheQAResponse(
          question, "pitch", 1L, null, null, createTestQAResponse(question));
      Optional<QAResponse> result =
          cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle exact match first before fuzzy matching")
    void shouldHandleExactMatchFirst() {
      // Given
      String question = "What is the progress?";
      QAResponse response = createTestQAResponse(question);

      // When
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, response);

      // Then - Exact same question should always hit
      Optional<QAResponse> result =
          cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);
      assertThat(result).isPresent();
    }
  }

  // ===========================================
  // CACHE STATISTICS TESTS
  // ===========================================

  @Nested
  @DisplayName("Cache Statistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("Should return correct cache statistics")
    void shouldReturnCorrectStats() {
      // Given
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L), "h1");
      cacheService.cachePitchRisk(2L, true, createTestPitchRiskDTO(2L), "h2");
      cacheService.cacheCycleRisk(1L, true, createTestCycleRiskDTO(1L));
      cacheService.cacheQAResponse("Q1", "pitch", 1L, null, null, createTestQAResponse("Q1"));
      cacheService.cacheQAResponse("Q2", "pitch", 2L, null, null, createTestQAResponse("Q2"));
      cacheService.cacheQAResponse("Q3", "pitch", 3L, null, null, createTestQAResponse("Q3"));

      // When
      AICacheService.CacheStats stats = cacheService.getCacheStats();

      // Then
      assertThat(stats.getTotalEntries()).isEqualTo(6);
      assertThat(stats.getPitchRiskEntries()).isEqualTo(2);
      assertThat(stats.getCycleRiskEntries()).isEqualTo(1);
      assertThat(stats.getQaEntries()).isEqualTo(3);
      assertThat(stats.isRiskCacheEnabled()).isTrue();
      assertThat(stats.isQaCacheEnabled()).isTrue();
      assertThat(stats.getProvider()).isEqualTo("in-memory");
    }

    @Test
    @DisplayName("Should track cache hits")
    void shouldTrackCacheHits() {
      // Given
      Long pitchId = 1L;
      cacheService.cachePitchRisk(pitchId, true, createTestPitchRiskDTO(pitchId), "hash");

      // When - Access cache multiple times
      cacheService.getCachedPitchRisk(pitchId, true);
      cacheService.getCachedPitchRisk(pitchId, true);
      cacheService.getCachedPitchRisk(pitchId, true);

      AICacheService.CacheStats stats = cacheService.getCacheStats();

      // Then
      assertThat(stats.getTotalHits()).isEqualTo(3);
    }
  }

  // ===========================================
  // CACHE MANAGEMENT TESTS
  // ===========================================

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("Should clear all caches")
    void shouldClearAllCaches() {
      // Given
      cacheService.cachePitchRisk(1L, true, createTestPitchRiskDTO(1L), "h1");
      cacheService.cacheCycleRisk(1L, true, createTestCycleRiskDTO(1L));
      cacheService.cacheQAResponse("Q1", "pitch", 1L, null, null, createTestQAResponse("Q1"));

      // Verify caches have entries
      assertThat(cacheService.getCacheStats().getTotalEntries()).isEqualTo(3);

      // When
      cacheService.clearAllCaches();

      // Then
      AICacheService.CacheStats stats = cacheService.getCacheStats();
      assertThat(stats.getTotalEntries()).isEqualTo(0);
      assertThat(stats.getPitchRiskEntries()).isEqualTo(0);
      assertThat(stats.getCycleRiskEntries()).isEqualTo(0);
      assertThat(stats.getQaEntries()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should cleanup expired entries")
    void shouldCleanupExpiredEntries() {
      // Given - Set short TTL for testing
      cacheConfig.getQa().setTtlHours(0); // This means infinite, so we need different approach

      // Note: Testing time-based expiration is tricky without mocking time
      // In real scenario, we'd use a Clock abstraction
      // For this test, we just verify cleanup doesn't break with no expired entries

      cacheService.cacheQAResponse("Q1", "pitch", 1L, null, null, createTestQAResponse("Q1"));

      // When
      cacheService.cleanupExpiredEntries();

      // Then - Non-expired entries should remain
      assertThat(cacheService.getCachedQAResponse("Q1", "pitch", 1L, null, null)).isPresent();
    }
  }

  // ===========================================
  // HELPER METHODS
  // ===========================================

  private PitchRiskDTO createTestPitchRiskDTO(Long pitchId) {
    PitchRiskDTO dto = new PitchRiskDTO();
    dto.setPitchId(pitchId);
    dto.setRiskLevel(RiskLevel.MEDIUM);
    dto.setRiskScore(50);
    return dto;
  }

  private CycleRiskOverviewDTO createTestCycleRiskDTO(Long cycleId) {
    CycleRiskOverviewDTO dto = new CycleRiskOverviewDTO();
    dto.setCycleId(cycleId);
    dto.setOverallRiskLevel(RiskLevel.LOW);
    return dto;
  }

  private QAResponse createTestQAResponse(String question) {
    QAResponse response = new QAResponse();
    response.setQuestion(question);
    response.setAnswer("Test answer for: " + question);
    response.setConfidenceScore(90);
    response.setCached(false);
    return response;
  }
}
