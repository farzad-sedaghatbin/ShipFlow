package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.qa.RAGEvaluationMetrics;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for RAGEvaluator. */
class RAGEvaluatorTest {

  private RAGEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new RAGEvaluator();
  }

  @Test
  void testEvaluateHighQualityRAG() {
    String question = "What is the current status of the payment feature?";
    String answer = "Based on the meeting notes [Source 1], the payment feature is currently in development. "
        + "The team completed the integration with the payment gateway [Source 2].";

    List<EmbeddingMatch<TextSegment>> retrievedDocs = createMockDocuments(
        List.of("Meeting notes: Payment feature is in development with 60% completion.",
            "Technical doc: Payment gateway integration completed last week."),
        List.of(0.85, 0.80));

    RAGEvaluationMetrics metrics = evaluator.evaluate(question, answer, retrievedDocs);

    assertThat(metrics.getRetrievalRelevance()).isGreaterThan(70.0);
    assertThat(metrics.getFaithfulness()).isGreaterThan(50.0);
    assertThat(metrics.getAnswerRelevance()).isGreaterThanOrEqualTo(40.0);
    assertThat(metrics.getDocumentsRetrieved()).isEqualTo(2);
    assertThat(metrics.getDocumentsCited()).isEqualTo(2);
  }

  @Test
  void testEvaluateNoInformationFound() {
    String question = "What is the deployment schedule?";
    String answer = "I couldn't find relevant information to answer your question.";

    List<EmbeddingMatch<TextSegment>> retrievedDocs = createMockDocuments(
        List.of("Unrelated document about testing"), List.of(0.45));

    RAGEvaluationMetrics metrics = evaluator.evaluate(question, answer, retrievedDocs);

    // High faithfulness for being honest about limitations
    assertThat(metrics.getFaithfulness()).isEqualTo(100.0);
    assertThat(metrics.getContextUtilization()).isLessThan(50.0);
  }

  @Test
  void testEvaluatePoorRelevance() {
    String question = "When is the next cycle starting?";
    String answer = "The next cycle will start on Monday.";

    List<EmbeddingMatch<TextSegment>> retrievedDocs = createMockDocuments(
        List.of("Meeting notes about design discussions"), List.of(0.30) // Low relevance
    );

    RAGEvaluationMetrics metrics = evaluator.evaluate(question, answer, retrievedDocs);

    assertThat(metrics.getRetrievalRelevance()).isLessThan(50.0);
  }

  @Test
  void testEvaluateNoDocuments() {
    String question = "Test question";
    String answer = "Test answer";
    List<EmbeddingMatch<TextSegment>> retrievedDocs = new ArrayList<>();

    RAGEvaluationMetrics metrics = evaluator.evaluate(question, answer, retrievedDocs);

    assertThat(metrics.getRetrievalRelevance()).isEqualTo(0.0);
    assertThat(metrics.getDocumentsRetrieved()).isEqualTo(0);
    assertThat(metrics.getDocumentsCited()).isEqualTo(0);
  }

  @Test
  void testAnswerRelevanceWithKeyTerms() {
    String question = "What are the risks in the authentication feature?";
    String answer = "The authentication feature has several risks: "
        + "1. Security vulnerabilities in password storage " + "2. Performance issues with token validation";

    List<EmbeddingMatch<TextSegment>> retrievedDocs = createMockDocuments(List.of("Risk assessment doc"),
        List.of(0.75));

    RAGEvaluationMetrics metrics = evaluator.evaluate(question, answer, retrievedDocs);

    // Should have high relevance since key terms (risks, authentication, feature)
    // are present
    assertThat(metrics.getAnswerRelevance()).isGreaterThanOrEqualTo(60.0);
  }

  private List<EmbeddingMatch<TextSegment>> createMockDocuments(List<String> texts, List<Double> scores) {
    List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

    for (int i = 0; i < texts.size(); i++) {
      @SuppressWarnings("unchecked")
      EmbeddingMatch<TextSegment> match = mock(EmbeddingMatch.class);
      TextSegment segment = TextSegment.from(texts.get(i));

      when(match.embedded()).thenReturn(segment);
      when(match.score()).thenReturn(scores.get(i));

      matches.add(match);
    }

    return matches;
  }
}
