package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;

/**
 * Tests for QAService recency boost functionality.
 * Recency boost increases the effective score of more recently updated documents.
 */
@ExtendWith(MockitoExtension.class)
class QAServiceRecencyBoostTest {

  @Mock
  private KnowledgeItemRepository knowledgeItemRepository;
  
  @Mock
  private QAInteractionRepository qaInteractionRepository;
  
  @Mock
  private AICacheService cacheService;
  
  @Mock
  private MessageService messageService;

  @InjectMocks
  private QAService qaService;
  
  private Method applyRecencyBoostMethod;

  @BeforeEach
  void setUp() throws Exception {
    // Get access to private method for testing
    applyRecencyBoostMethod = QAService.class.getDeclaredMethod("applyRecencyBoost", List.class);
    applyRecencyBoostMethod.setAccessible(true);
  }

  private EmbeddingMatch<TextSegment> createMatch(double score, String title, LocalDateTime updatedAt) {
    Metadata metadata = new Metadata();
    metadata.put("title", title);
    if (updatedAt != null) {
      metadata.put("updatedAt", updatedAt.toString());
    }
    TextSegment segment = TextSegment.from("Test content for " + title, metadata);
    return new EmbeddingMatch<>(score, "embed-" + title, null, segment);
  }

  @SuppressWarnings("unchecked")
  private List<EmbeddingMatch<TextSegment>> invokeRecencyBoost(List<EmbeddingMatch<TextSegment>> matches) 
      throws Exception {
    return (List<EmbeddingMatch<TextSegment>>) applyRecencyBoostMethod.invoke(qaService, matches);
  }

  @Nested
  @DisplayName("Recency Boost Application")
  class RecencyBoostTests {

    @Test
    @DisplayName("Should boost documents updated within 7 days by 15%")
    void shouldBoostRecentDocuments() throws Exception {
      LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
      EmbeddingMatch<TextSegment> recentMatch = createMatch(0.8, "Recent Doc", threeDaysAgo);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(recentMatch));
      
      assertThat(result).hasSize(1);
      // 0.8 * 1.15 = 0.92
      assertThat(result.get(0).score()).isGreaterThan(0.9);
      assertThat(result.get(0).score()).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("Should boost documents updated within 30 days by 10%")
    void shouldBoostMonthOldDocuments() throws Exception {
      LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);
      EmbeddingMatch<TextSegment> match = createMatch(0.8, "Month Old Doc", fifteenDaysAgo);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(match));
      
      assertThat(result).hasSize(1);
      // 0.8 * 1.10 = 0.88
      assertThat(result.get(0).score()).isGreaterThan(0.87);
      assertThat(result.get(0).score()).isLessThan(0.9);
    }

    @Test
    @DisplayName("Should boost documents updated within 90 days by 5%")
    void shouldBoostQuarterOldDocuments() throws Exception {
      LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
      EmbeddingMatch<TextSegment> match = createMatch(0.8, "Quarter Old Doc", sixtyDaysAgo);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(match));
      
      assertThat(result).hasSize(1);
      // 0.8 * 1.05 = 0.84
      assertThat(result.get(0).score()).isGreaterThan(0.83);
      assertThat(result.get(0).score()).isLessThan(0.86);
    }

    @Test
    @DisplayName("Should not boost old documents")
    void shouldNotBoostOldDocuments() throws Exception {
      LocalDateTime sixMonthsAgo = LocalDateTime.now().minusDays(180);
      EmbeddingMatch<TextSegment> match = createMatch(0.8, "Old Doc", sixMonthsAgo);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(match));
      
      assertThat(result).hasSize(1);
      // No boost - score should remain 0.8
      assertThat(result.get(0).score()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("Should cap boosted score at 1.0")
    void shouldCapScoreAtOne() throws Exception {
      LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
      EmbeddingMatch<TextSegment> match = createMatch(0.95, "High Score Doc", yesterday);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(match));
      
      assertThat(result).hasSize(1);
      // 0.95 * 1.15 = 1.0925, but capped at 1.0
      assertThat(result.get(0).score()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should reorder by boosted scores")
    void shouldReorderByBoostedScores() throws Exception {
      LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
      LocalDateTime sixMonthsAgo = LocalDateTime.now().minusDays(180);
      
      // Old doc has higher base score
      EmbeddingMatch<TextSegment> oldDoc = createMatch(0.9, "Old High Score", sixMonthsAgo);
      // Recent doc has lower base score but will be boosted
      EmbeddingMatch<TextSegment> recentDoc = createMatch(0.8, "Recent Lower Score", yesterday);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(oldDoc, recentDoc));
      
      assertThat(result).hasSize(2);
      // Recent doc (0.8 * 1.15 = 0.92) should now be first
      // Old doc stays at 0.9
      assertThat(result.get(0).embedded().metadata().getString("title")).isEqualTo("Recent Lower Score");
      assertThat(result.get(1).embedded().metadata().getString("title")).isEqualTo("Old High Score");
    }

    @Test
    @DisplayName("Should handle missing updatedAt metadata")
    void shouldHandleMissingUpdatedAt() throws Exception {
      EmbeddingMatch<TextSegment> matchWithoutDate = createMatch(0.8, "No Date Doc", null);
      
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList(matchWithoutDate));
      
      assertThat(result).hasSize(1);
      // No boost without date - score should remain 0.8
      assertThat(result.get(0).score()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("Should handle empty list")
    void shouldHandleEmptyList() throws Exception {
      List<EmbeddingMatch<TextSegment>> result = invokeRecencyBoost(Arrays.asList());
      
      assertThat(result).isEmpty();
    }
  }
}
