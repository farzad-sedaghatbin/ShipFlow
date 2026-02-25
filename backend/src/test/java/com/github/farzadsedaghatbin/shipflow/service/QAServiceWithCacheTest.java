package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.config.QAConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Tests for QAService with caching integration. Tests cover: - Cache hit
 * scenarios - Cache miss scenarios - Fuzzy matching for similar questions -
 * Cache invalidation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QAService Tests with Caching")
class QAServiceWithCacheTest {

  @Mock
  private KnowledgeItemRepository knowledgeItemRepository;

  @Mock
  private QAInteractionRepository qaInteractionRepository;

  @Mock
  private EmbeddingModel embeddingModel;

  @Mock
  private EmbeddingStore<TextSegment> embeddingStore;

  @Mock
  private ChatLanguageModel chatLanguageModel;

  @Mock
  private KnowledgeIngestionService knowledgeIngestionService;

  @Mock
  private QAConfig qaConfig;

  @Mock
  private AIConfig aiConfig;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ObjectMapper objectMapper;

  private AICacheConfig cacheConfig;
  private AICacheService cacheService;

  @BeforeEach
  void setUp() {
    // Setup cache config
    cacheConfig = new AICacheConfig();
    cacheConfig.setEnabled(true);
    cacheConfig.setProvider("in-memory");

    AICacheConfig.QACacheConfig qaCache = new AICacheConfig.QACacheConfig();
    qaCache.setEnabled(true);
    qaCache.setTtlHours(3);
    qaCache.setSimilarityThreshold(0.85);
    qaCache.setMaxEntriesPerContext(100);
    cacheConfig.setQa(qaCache);

    AICacheConfig.RiskCacheConfig riskCache = new AICacheConfig.RiskCacheConfig();
    riskCache.setEnabled(true);
    cacheConfig.setRisk(riskCache);

    cacheService = new AICacheService(cacheConfig, redisTemplate, objectMapper);
    cacheService.init();
  }

  @Nested
  @DisplayName("Q&A Cache Hit Tests")
  class QACacheHitTests {

    @Test
    @DisplayName("Should return cached response for exact same question")
    void shouldReturnCachedResponseForExactQuestion() {
      // Given
      String question = "What is the progress of pitch 1?";
      QAResponse cachedResponse = QAResponse.builder().question(question).answer("The pitch is 50% complete")
          .confidenceScore(90).cached(false).answeredAt(LocalDateTime.now()).build();

      // Cache the response
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, cachedResponse);

      // When
      Optional<QAResponse> result = cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getAnswer()).isEqualTo("The pitch is 50% complete");
    }

    @Test
    @DisplayName("Should return cached response for fuzzy matching question")
    void shouldReturnCachedResponseForFuzzyMatch() {
      // Given
      String originalQuestion = "What is the progress of this pitch?";
      String similarQuestion = "What is the current progress of this pitch?";

      QAResponse cachedResponse = QAResponse.builder().question(originalQuestion)
          .answer("The pitch is making good progress").confidenceScore(90).cached(false)
          .answeredAt(LocalDateTime.now()).build();

      // Cache the response
      cacheService.cacheQAResponse(originalQuestion, "pitch", 1L, null, null, cachedResponse);

      // When
      Optional<QAResponse> result = cacheService.getCachedQAResponse(similarQuestion, "pitch", 1L, null, null);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getAnswer()).isEqualTo("The pitch is making good progress");
    }

    @Test
    @DisplayName("Should not return cached response for very different question")
    void shouldNotReturnCachedResponseForDifferentQuestion() {
      // Given
      String originalQuestion = "What is the progress?";
      String differentQuestion = "How many team members are assigned?";

      QAResponse cachedResponse = QAResponse.builder().question(originalQuestion).answer("Progress is good")
          .confidenceScore(90).cached(false).answeredAt(LocalDateTime.now()).build();

      cacheService.cacheQAResponse(originalQuestion, "pitch", 1L, null, null, cachedResponse);

      // When
      Optional<QAResponse> result = cacheService.getCachedQAResponse(differentQuestion, "pitch", 1L, null, null);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Q&A Cache Context Isolation Tests")
  class QACacheContextTests {

    @Test
    @DisplayName("Should isolate cache by pitch context")
    void shouldIsolateCacheByPitchContext() {
      // Given
      String question = "What is the status?";

      QAResponse pitch1Response = QAResponse.builder().question(question).answer("Pitch 1 is in progress")
          .confidenceScore(90).cached(false).answeredAt(LocalDateTime.now()).build();

      QAResponse pitch2Response = QAResponse.builder().question(question).answer("Pitch 2 is completed")
          .confidenceScore(90).cached(false).answeredAt(LocalDateTime.now()).build();

      // Cache responses for different pitches
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, pitch1Response);
      cacheService.cacheQAResponse(question, "pitch", 2L, null, null, pitch2Response);

      // When
      Optional<QAResponse> result1 = cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);
      Optional<QAResponse> result2 = cacheService.getCachedQAResponse(question, "pitch", 2L, null, null);

      // Then
      assertThat(result1).isPresent();
      assertThat(result1.get().getAnswer()).isEqualTo("Pitch 1 is in progress");

      assertThat(result2).isPresent();
      assertThat(result2.get().getAnswer()).isEqualTo("Pitch 2 is completed");
    }

    @Test
    @DisplayName("Should isolate cache by cycle context")
    void shouldIsolateCacheByCycleContext() {
      // Given
      String question = "What are the risks?";

      QAResponse cycle1Response = QAResponse.builder().question(question).answer("Cycle 1 has low risks")
          .confidenceScore(90).cached(false).answeredAt(LocalDateTime.now()).build();

      // Cache response for cycle 1
      cacheService.cacheQAResponse(question, "cycle", 1L, 1L, null, cycle1Response);

      // When - Ask for different cycle
      Optional<QAResponse> result = cacheService.getCachedQAResponse(question, "cycle", 2L, 2L, null);

      // Then - Should not find cached response for different cycle
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Q&A Cache Invalidation Tests")
  class QACacheInvalidationTests {

    @Test
    @DisplayName("Should invalidate cache for specific context")
    void shouldInvalidateCacheForContext() {
      // Given
      String question1 = "Question 1";
      String question2 = "Question 2";

      QAResponse response1 = createTestResponse(question1);
      QAResponse response2 = createTestResponse(question2);
      QAResponse response3 = createTestResponse("Question 3");

      cacheService.cacheQAResponse(question1, "pitch", 1L, null, null, response1);
      cacheService.cacheQAResponse(question2, "pitch", 1L, null, null, response2);
      cacheService.cacheQAResponse("Question 3", "pitch", 2L, null, null, response3);

      // When - Invalidate pitch 1 cache
      cacheService.invalidateQACache("pitch", 1L);

      // Then - Pitch 1 cache should be invalidated
      assertThat(cacheService.getCachedQAResponse(question1, "pitch", 1L, null, null)).isEmpty();
      assertThat(cacheService.getCachedQAResponse(question2, "pitch", 1L, null, null)).isEmpty();

      // Pitch 2 should still be cached
      assertThat(cacheService.getCachedQAResponse("Question 3", "pitch", 2L, null, null)).isPresent();
    }

    @Test
    @DisplayName("Should clear all Q&A caches")
    void shouldClearAllCaches() {
      // Given
      cacheService.cacheQAResponse("Q1", "pitch", 1L, null, null, createTestResponse("Q1"));
      cacheService.cacheQAResponse("Q2", "cycle", 1L, 1L, null, createTestResponse("Q2"));
      cacheService.cacheQAResponse("Q3", "team", null, null, 1L, createTestResponse("Q3"));

      // When
      cacheService.clearAllCaches();

      // Then
      AICacheService.CacheStats stats = cacheService.getCacheStats();
      assertThat(stats.getQaEntries()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Q&A Cache Disabled Tests")
  class QACacheDisabledTests {

    @Test
    @DisplayName("Should not cache when Q&A cache is disabled")
    void shouldNotCacheWhenDisabled() {
      // Given
      cacheConfig.getQa().setEnabled(false);

      String question = "Test question";
      QAResponse response = createTestResponse(question);

      // When
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, response);
      Optional<QAResponse> result = cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not cache when master cache is disabled")
    void shouldNotCacheWhenMasterDisabled() {
      // Given
      cacheConfig.setEnabled(false);

      String question = "Test question";
      QAResponse response = createTestResponse(question);

      // When
      cacheService.cacheQAResponse(question, "pitch", 1L, null, null, response);
      Optional<QAResponse> result = cacheService.getCachedQAResponse(question, "pitch", 1L, null, null);

      // Then
      assertThat(result).isEmpty();
    }
  }

  private QAResponse createTestResponse(String question) {
    return QAResponse.builder().question(question).answer("Test answer for: " + question).confidenceScore(90)
        .cached(false).answeredAt(LocalDateTime.now()).build();
  }
}
