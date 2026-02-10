package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.QAResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnswerGenerationServiceTest {

  @Mock
  private ChatLanguageModel chatLanguageModel;

  @Mock
  private LLMCacheService llmCacheService;

  @Mock
  private PromptCompressor promptCompressor;

  @Mock
  private ContentGuardrails contentGuardrails;

  @Mock
  private FeedbackLearningService feedbackLearningService;

  @Mock
  private RAGEvaluator ragEvaluator;

  @Mock
  private ConversationManager conversationManager;

  private AnswerGenerationService service;

  @BeforeEach
  void setUp() {
    service = new AnswerGenerationService(
        chatLanguageModel,
        llmCacheService,
        promptCompressor,
        contentGuardrails,
        feedbackLearningService,
        ragEvaluator,
        conversationManager);
  }

  // ===== buildPrompt tests =====

  @Test
  void buildPrompt_basicQuestion_containsQuestionAndContext() {
    // Given
    String question = "What are the risks?";
    String context = "The project has budget risks.";

    // When
    String prompt = service.buildPrompt(question, context, "");

    // Then
    assertThat(prompt).contains(question);
    assertThat(prompt).contains(context);
    assertThat(prompt).contains("ShipFlow");
    assertThat(prompt).contains("Shape Up");
  }

  @Test
  void buildPrompt_withConversationHistory_includesHistory() {
    // Given
    String question = "What else?";
    String context = "Some context";
    String history = "Previous Q: What about cycle 5? Previous A: It has 3 pitches.";

    // When
    String prompt = service.buildPrompt(question, context, history);

    // Then
    assertThat(prompt).contains(history);
  }

  // ===== calculateConfidenceScore tests =====

  @Test
  void calculateConfidenceScore_emptyMatches_returnsZero() {
    // When
    int score = service.calculateConfidenceScore(Collections.emptyList());

    // Then
    assertThat(score).isZero();
  }

  @Test
  void calculateConfidenceScore_singleHighScoreMatch_returnsHighScore() {
    // Given
    TextSegment segment = TextSegment.from("content");
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id1", null, segment);

    // When
    int score = service.calculateConfidenceScore(List.of(match));

    // Then
    assertThat(score).isGreaterThanOrEqualTo(90);
    assertThat(score).isLessThanOrEqualTo(100);
  }

  @Test
  void calculateConfidenceScore_multipleMatches_includesSourceBonus() {
    // Given
    TextSegment segment = TextSegment.from("content");
    EmbeddingMatch<TextSegment> match1 = new EmbeddingMatch<>(0.8, "id1", null, segment);
    EmbeddingMatch<TextSegment> match2 = new EmbeddingMatch<>(0.8, "id2", null, segment);
    EmbeddingMatch<TextSegment> match3 = new EmbeddingMatch<>(0.8, "id3", null, segment);

    // When
    int scoreWithMultiple = service.calculateConfidenceScore(List.of(match1, match2, match3));
    int scoreWithSingle = service.calculateConfidenceScore(List.of(match1));

    // Then
    assertThat(scoreWithMultiple).isGreaterThan(scoreWithSingle);
  }

  @Test
  void calculateConfidenceScore_cappedAt100() {
    // Given - very high scores
    TextSegment segment = TextSegment.from("content");
    List<EmbeddingMatch<TextSegment>> matches = List.of(
        new EmbeddingMatch<>(0.99, "id1", null, segment),
        new EmbeddingMatch<>(0.99, "id2", null, segment),
        new EmbeddingMatch<>(0.99, "id3", null, segment),
        new EmbeddingMatch<>(0.99, "id4", null, segment),
        new EmbeddingMatch<>(0.99, "id5", null, segment));

    // When
    int score = service.calculateConfidenceScore(matches);

    // Then
    assertThat(score).isLessThanOrEqualTo(100);
  }

  // ===== generateSuggestedFollowUps tests =====

  @Test
  void generateSuggestedFollowUps_pitchContext_returnsPitchSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("pitch");

    // When
    List<String> suggestions = service.generateSuggestedFollowUps(request, Collections.emptyList());

    // Then
    assertThat(suggestions).hasSize(3);
    assertThat(suggestions).anyMatch(s -> s.toLowerCase().contains("pitch") || s.toLowerCase().contains("status"));
  }

  @Test
  void generateSuggestedFollowUps_cycleContext_returnsCycleSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("cycle");

    // When
    List<String> suggestions = service.generateSuggestedFollowUps(request, Collections.emptyList());

    // Then
    assertThat(suggestions).hasSize(3);
    assertThat(suggestions).anyMatch(s -> s.toLowerCase().contains("cycle") || s.toLowerCase().contains("pitch"));
  }

  @Test
  void generateSuggestedFollowUps_teamContext_returnsTeamSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("team");

    // When
    List<String> suggestions = service.generateSuggestedFollowUps(request, Collections.emptyList());

    // Then
    assertThat(suggestions).hasSize(2);
    assertThat(suggestions).anyMatch(s -> s.toLowerCase().contains("team"));
  }

  @Test
  void generateSuggestedFollowUps_meetingContext_returnsMeetingSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("meeting");

    // When
    List<String> suggestions = service.generateSuggestedFollowUps(request, Collections.emptyList());

    // Then
    assertThat(suggestions).hasSize(2);
    assertThat(suggestions).anyMatch(s -> s.toLowerCase().contains("meeting") 
        || s.toLowerCase().contains("decision") 
        || s.toLowerCase().contains("action"));
  }

  @Test
  void generateSuggestedFollowUps_noContext_returnsDefaultSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();

    // When
    List<String> suggestions = service.generateSuggestedFollowUps(request, Collections.emptyList());

    // Then
    assertThat(suggestions).hasSize(3);
  }

  // ===== buildCitations tests =====

  @Test
  void buildCitations_emptyMatches_returnsEmptyList() {
    // When
    List<QAResponse.SourceCitation> citations = service.buildCitations(Collections.emptyList());

    // Then
    assertThat(citations).isEmpty();
  }

  @Test
  void buildCitations_matchWithMetadata_extractsFields() {
    // Given
    Metadata metadata = new Metadata()
        .put("knowledgeItemId", "123")
        .put("entityType", "pitch")
        .put("entityId", "456")
        .put("title", "Payment Feature");
    TextSegment segment = TextSegment.from("This is the content about payments.", metadata);
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "emb-id", null, segment);

    // When
    List<QAResponse.SourceCitation> citations = service.buildCitations(List.of(match));

    // Then
    assertThat(citations).hasSize(1);
    QAResponse.SourceCitation citation = citations.get(0);
    assertThat(citation.getKnowledgeItemId()).isEqualTo(123L);
    assertThat(citation.getEntityType()).isEqualTo("pitch");
    assertThat(citation.getEntityId()).isEqualTo(456L);
    assertThat(citation.getTitle()).isEqualTo("Payment Feature");
    assertThat(citation.getRelevanceScore()).isEqualTo(0.85);
  }

  @Test
  void buildCitations_longContent_truncatesSnippet() {
    // Given
    String longContent = "A".repeat(300);
    TextSegment segment = TextSegment.from(longContent);
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id", null, segment);

    // When
    List<QAResponse.SourceCitation> citations = service.buildCitations(List.of(match));

    // Then
    assertThat(citations.get(0).getSnippet().length()).isLessThanOrEqualTo(203); // 200 + "..."
  }

  // ===== buildSourceIds tests =====

  @Test
  void buildSourceIds_emptyMatches_returnsEmptyString() {
    // When
    String ids = service.buildSourceIds(Collections.emptyList());

    // Then
    assertThat(ids).isEmpty();
  }

  @Test
  void buildSourceIds_multipleMatches_returnsCommaSeparated() {
    // Given
    Metadata meta1 = new Metadata().put("knowledgeItemId", "1");
    Metadata meta2 = new Metadata().put("knowledgeItemId", "2");
    Metadata meta3 = new Metadata().put("knowledgeItemId", "3");
    List<EmbeddingMatch<TextSegment>> matches = List.of(
        new EmbeddingMatch<>(0.8, "id1", null, TextSegment.from("c1", meta1)),
        new EmbeddingMatch<>(0.8, "id2", null, TextSegment.from("c2", meta2)),
        new EmbeddingMatch<>(0.8, "id3", null, TextSegment.from("c3", meta3)));

    // When
    String ids = service.buildSourceIds(matches);

    // Then
    assertThat(ids).isEqualTo("1,2,3");
  }

  // ===== isLLMAvailable tests =====

  @Test
  void isLLMAvailable_withChatModel_returnsTrue() {
    assertThat(service.isLLMAvailable()).isTrue();
  }

  @Test
  void isLLMAvailable_withoutChatModel_returnsFalse() {
    // Given
    service = new AnswerGenerationService(null, null, null, null, null, null, null);

    // Then
    assertThat(service.isLLMAvailable()).isFalse();
  }

  // ===== generate tests =====

  @Test
  void generate_withLLMAndContext_generatesAnswer() {
    // Given
    String question = "What are the risks?";
    String context = "Budget risks and timeline risks.";
    when(chatLanguageModel.generate(anyString())).thenReturn("The main risks are budget and timeline.");
    when(promptCompressor.compress(anyString())).thenAnswer(i -> i.getArgument(0));
    when(contentGuardrails.validate(anyString(), anyDouble()))
        .thenReturn(new ContentGuardrails.GuardrailResult(true, Collections.emptyList(), 85));
    when(feedbackLearningService.getPatternSuccessRate(anyString())).thenReturn(0.8);

    TextSegment segment = TextSegment.from("content");
    List<EmbeddingMatch<TextSegment>> matches = List.of(
        new EmbeddingMatch<>(0.85, "id1", null, segment));

    // When
    AnswerGenerationService.GenerationResult result = service.generate(
        question, context, "", matches, null);

    // Then
    assertThat(result.getAnswer()).isEqualTo("The main risks are budget and timeline.");
    assertThat(result.getConfidenceScore()).isGreaterThan(0);
  }

  @Test
  void generate_withEmptyContext_returnsNoInfoMessage() {
    // Given - service without LLM
    service = new AnswerGenerationService(null, null, null, null, null, null, null);

    // When
    AnswerGenerationService.GenerationResult result = service.generate(
        "What are risks?", "", "", Collections.emptyList(), null);

    // Then
    assertThat(result.getAnswer()).contains("couldn't find relevant information");
    assertThat(result.getConfidenceScore()).isZero();
  }

  @Test
  void generate_withCachedResponse_usesCachedAnswer() {
    // Given
    String question = "What are the risks?";
    String context = "Some context";
    String cachedAnswer = "Cached answer from previous query";
    when(promptCompressor.compress(anyString())).thenAnswer(i -> i.getArgument(0));
    when(llmCacheService.getCachedResponse(anyString())).thenReturn(cachedAnswer);
    when(contentGuardrails.validate(anyString(), anyDouble()))
        .thenReturn(new ContentGuardrails.GuardrailResult(true, Collections.emptyList(), 85));
    when(feedbackLearningService.getPatternSuccessRate(anyString())).thenReturn(0.8);

    TextSegment segment = TextSegment.from("content");
    List<EmbeddingMatch<TextSegment>> matches = List.of(
        new EmbeddingMatch<>(0.85, "id1", null, segment));

    // When
    AnswerGenerationService.GenerationResult result = service.generate(
        question, context, "", matches, null);

    // Then
    assertThat(result.getAnswer()).isEqualTo(cachedAnswer);
  }

  @Test
  void generate_withLowPatternSuccessRate_reducesConfidence() {
    // Given
    String context = "Some context";
    when(chatLanguageModel.generate(anyString())).thenReturn("Answer");
    when(promptCompressor.compress(anyString())).thenAnswer(i -> i.getArgument(0));
    when(feedbackLearningService.getPatternSuccessRate(anyString())).thenReturn(0.3); // Low success
    when(contentGuardrails.validate(anyString(), anyDouble()))
        .thenReturn(new ContentGuardrails.GuardrailResult(true, Collections.emptyList(), 76));

    TextSegment segment = TextSegment.from("content");
    List<EmbeddingMatch<TextSegment>> matches = List.of(
        new EmbeddingMatch<>(0.85, "id1", null, segment));

    // When
    AnswerGenerationService.GenerationResult result = service.generate(
        "Question", context, "", matches, null);

    // Then - confidence should be reduced due to low pattern success
    assertThat(result.getConfidenceScore()).isLessThanOrEqualTo(85);
  }
}
