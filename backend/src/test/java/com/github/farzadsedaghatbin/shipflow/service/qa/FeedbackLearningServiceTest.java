package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackLearningServiceTest {

  @Mock private QAInteractionRepository qaInteractionRepository;

  @Mock private AICacheConfig cacheConfig;

  private FeedbackLearningService feedbackLearningService;

  @BeforeEach
  void setUp() {
    // Configure mock to use in-memory provider (avoid Redis initialization in tests)
    lenient().when(cacheConfig.isRedisProvider()).thenReturn(false);
    feedbackLearningService = new FeedbackLearningService(qaInteractionRepository, cacheConfig);
  }

  @Test
  void recordFeedback_validFeedback_savesInteraction() {
    // Given
    Long interactionId = 1L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(interactionId);
    interaction.setQuestion("What are the payment risks?");

    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

    // When
    feedbackLearningService.recordFeedback(interactionId, true, "Very helpful!");

    // Then
    verify(qaInteractionRepository).findById(interactionId);
    verify(qaInteractionRepository).save(interaction);
    assertThat(interaction.getFeedbackHelpful()).isTrue();
    assertThat(interaction.getFeedbackText()).isEqualTo("Very helpful!");
  }

  @Test
  void recordFeedback_interactionNotFound_throwsException() {
    // Given
    Long interactionId = 999L;
    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> feedbackLearningService.recordFeedback(interactionId, true, "Great!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Q&A interaction not found");
  }

  @Test
  void recordFeedback_updatesPatternStats() {
    // Given
    Long interactionId = 1L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(interactionId);
    interaction.setQuestion("What are the payment risks?");
    interaction.setSourceKnowledgeIds("pitch_123,cycle_456");

    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

    // When - record multiple feedbacks to build stats
    feedbackLearningService.recordFeedback(interactionId, true, "Great answer!");

    // Pattern stats are updated internally
    double successRate =
        feedbackLearningService.getPatternSuccessRate("What are the payment risks?");

    // Then
    assertThat(successRate).isGreaterThan(0.0);
  }

  @Test
  void getPatternSuccessRate_unknownPattern_returnsDefaultRate() {
    // When
    double successRate = feedbackLearningService.getPatternSuccessRate("Never seen before?");

    // Then
    assertThat(successRate).isEqualTo(0.5); // Default neutral success rate
  }

  @Test
  void recordFeedback_updatesSourceStats_helpfulFeedback() {
    // Given
    Long interactionId = 1L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(interactionId);
    interaction.setQuestion("What are the risks?");
    interaction.setSourceKnowledgeIds("pitch_123,cycle_456");

    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

    // When
    feedbackLearningService.recordFeedback(interactionId, true, null);
    double boost1 = feedbackLearningService.getSourceRelevanceBoost("pitch_123");
    double boost2 = feedbackLearningService.getSourceRelevanceBoost("cycle_456");

    // Then
    assertThat(boost1).isGreaterThan(1.0);
    assertThat(boost2).isGreaterThan(1.0);
  }

  @Test
  void recordFeedback_updatesSourceStats_unhelpfulFeedback() {
    // Given
    Long interactionId = 2L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(interactionId);
    interaction.setQuestion("What are the risks?");
    interaction.setSourceKnowledgeIds("pitch_789");

    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

    // When
    feedbackLearningService.recordFeedback(interactionId, false, null);
    double boost = feedbackLearningService.getSourceRelevanceBoost("pitch_789");

    // Then
    assertThat(boost).isLessThan(1.0);
  }

  @Test
  void getSourceRelevanceBoost_multipleInteractions_calculatesCorrectBoost() {
    // Given - Record multiple interactions with mixed feedback
    for (int i = 0; i < 3; i++) {
      Long id = (long) i;
      QAInteraction interaction = new QAInteraction();
      interaction.setId(id);
      interaction.setQuestion("Question " + i);
      interaction.setSourceKnowledgeIds("pitch_100");

      when(qaInteractionRepository.findById(id)).thenReturn(Optional.of(interaction));
      when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

      feedbackLearningService.recordFeedback(id, true, null);
    }

    // One unhelpful
    Long id = 10L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(id);
    interaction.setQuestion("Question 10");
    interaction.setSourceKnowledgeIds("pitch_100");
    when(qaInteractionRepository.findById(id)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);
    feedbackLearningService.recordFeedback(id, false, null);

    // When
    double boost = feedbackLearningService.getSourceRelevanceBoost("pitch_100");

    // Then - 3 helpful, 1 unhelpful = 75% success rate
    assertThat(boost).isBetween(1.0, 1.2);
  }

  @Test
  void getSourceRelevanceBoost_unknownSource_returnsNeutralBoost() {
    // When
    double boost = feedbackLearningService.getSourceRelevanceBoost("unknown_source");

    // Then
    assertThat(boost).isEqualTo(1.0); // Neutral boost for unknown sources
  }

  @Test
  void getSourceRelevanceBoost_allNegativeFeedback_returnsLowBoost() {
    // Given - Record multiple negative feedbacks
    for (int i = 0; i < 3; i++) {
      Long id = 100L + i;
      QAInteraction interaction = new QAInteraction();
      interaction.setId(id);
      interaction.setQuestion("Question " + i);
      interaction.setSourceKnowledgeIds("bad_source");

      when(qaInteractionRepository.findById(id)).thenReturn(Optional.of(interaction));
      when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

      feedbackLearningService.recordFeedback(id, false, null);
    }

    // When
    double boost = feedbackLearningService.getSourceRelevanceBoost("bad_source");

    // Then - Should be minimum boost (0.8)
    assertThat(boost).isLessThanOrEqualTo(0.8);
  }

  @Test
  void getSourceRelevanceBoost_allPositiveFeedback_returnsHighBoost() {
    // Given - Record multiple positive feedbacks
    for (int i = 0; i < 3; i++) {
      Long id = 200L + i;
      QAInteraction interaction = new QAInteraction();
      interaction.setId(id);
      interaction.setQuestion("Question " + i);
      interaction.setSourceKnowledgeIds("great_source");

      when(qaInteractionRepository.findById(id)).thenReturn(Optional.of(interaction));
      when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

      feedbackLearningService.recordFeedback(id, true, null);
    }

    // When
    double boost = feedbackLearningService.getSourceRelevanceBoost("great_source");

    // Then - Should be maximum boost (1.2)
    assertThat(boost).isGreaterThanOrEqualTo(1.2);
  }

  @Test
  void recordFeedback_nullText_acceptsNullFeedback() {
    // Given
    Long interactionId = 1L;
    QAInteraction interaction = new QAInteraction();
    interaction.setId(interactionId);
    interaction.setQuestion("What are the risks?");

    when(qaInteractionRepository.findById(interactionId)).thenReturn(Optional.of(interaction));
    when(qaInteractionRepository.save(any(QAInteraction.class))).thenReturn(interaction);

    // When
    feedbackLearningService.recordFeedback(interactionId, false, null);

    // Then
    verify(qaInteractionRepository).save(interaction);
    assertThat(interaction.getFeedbackHelpful()).isFalse();
    assertThat(interaction.getFeedbackText()).isNull();
  }
}
