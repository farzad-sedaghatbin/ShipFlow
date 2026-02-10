package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionProcessingServiceTest {

  @Mock
  private QueryDecomposer queryDecomposer;

  private QuestionProcessingService service;

  @BeforeEach
  void setUp() {
    service = new QuestionProcessingService(queryDecomposer);
  }

  // ===== extractSearchTerms tests =====

  @Test
  void extractSearchTerms_cyclePattern_extractsCycleAndNumber() {
    // Given
    String question = "What pitches are in cycle 5?";

    // When
    List<String> terms = service.extractSearchTerms(question);

    // Then
    assertThat(terms).contains("cycle 5", "pitches");
  }

  @Test
  void extractSearchTerms_atRiskStatus_extractsMultipleForms() {
    // Given
    String question = "Show me at-risk pitches";

    // When
    List<String> terms = service.extractSearchTerms(question);

    // Then
    assertThat(terms).contains("at-risk", "at_risk", "risk");
  }

  @Test
  void extractSearchTerms_inProgressStatus_extractsMultipleForms() {
    // Given
    String question = "Which pitches are in progress?";

    // When
    List<String> terms = service.extractSearchTerms(question);

    // Then
    assertThat(terms).contains("in_progress", "in progress", "progress");
  }

  @Test
  void extractSearchTerms_completedStatus_extractsMultipleForms() {
    // Given
    String question = "List completed tasks";

    // When
    List<String> terms = service.extractSearchTerms(question);

    // Then
    assertThat(terms).contains("completed", "complete", "done");
  }

  @Test
  void extractSearchTerms_emptyQuestion_returnsEmptyList() {
    // When
    List<String> terms = service.extractSearchTerms("");

    // Then
    assertThat(terms).isEmpty();
  }

  @Test
  void extractSearchTerms_nullQuestion_returnsEmptyList() {
    // When
    List<String> terms = service.extractSearchTerms(null);

    // Then
    assertThat(terms).isEmpty();
  }

  @Test
  void extractSearchTerms_standaloneNumber_extractsNumber() {
    // Given
    String question = "Tell me about pitch 42";

    // When
    List<String> terms = service.extractSearchTerms(question);

    // Then
    assertThat(terms).contains("42", "pitch");
  }

  // ===== isLikelyIdOnlyQuery tests =====

  @Test
  void isLikelyIdOnlyQuery_justNumber_returnsTrue() {
    assertThat(service.isLikelyIdOnlyQuery("4")).isTrue();
    assertThat(service.isLikelyIdOnlyQuery("42")).isTrue();
  }

  @Test
  void isLikelyIdOnlyQuery_entityWithNumber_returnsTrue() {
    assertThat(service.isLikelyIdOnlyQuery("cycle 4")).isTrue();
    assertThat(service.isLikelyIdOnlyQuery("pitch 15")).isTrue();
    assertThat(service.isLikelyIdOnlyQuery("team 2")).isTrue();
    assertThat(service.isLikelyIdOnlyQuery("meeting 3")).isTrue();
  }

  @Test
  void isLikelyIdOnlyQuery_withNameLikeText_returnsFalse() {
    assertThat(service.isLikelyIdOnlyQuery("Cycle 4 Planning")).isFalse();
    assertThat(service.isLikelyIdOnlyQuery("Sprint 2 Build Phase")).isFalse();
  }

  @Test
  void isLikelyIdOnlyQuery_nullQuestion_returnsFalse() {
    assertThat(service.isLikelyIdOnlyQuery(null)).isFalse();
  }

  // ===== extractEntityIdFromQuestion tests =====

  @Test
  void extractEntityIdFromQuestion_cyclePattern_extractsId() {
    // When
    Long id = service.extractEntityIdFromQuestion("cycle 5", "cycle");

    // Then
    assertThat(id).isEqualTo(5L);
  }

  @Test
  void extractEntityIdFromQuestion_pitchPattern_extractsId() {
    // When
    Long id = service.extractEntityIdFromQuestion("pitch 42", "pitch");

    // Then
    assertThat(id).isEqualTo(42L);
  }

  @Test
  void extractEntityIdFromQuestion_justNumber_extractsId() {
    // When
    Long id = service.extractEntityIdFromQuestion("15", "pitch");

    // Then
    assertThat(id).isEqualTo(15L);
  }

  @Test
  void extractEntityIdFromQuestion_inForAboutPattern_extractsId() {
    // When
    Long id = service.extractEntityIdFromQuestion("in cycle 7", "cycle");

    // Then
    assertThat(id).isEqualTo(7L);
  }

  @Test
  void extractEntityIdFromQuestion_noMatch_returnsNull() {
    // When
    Long id = service.extractEntityIdFromQuestion("what is this", "cycle");

    // Then
    assertThat(id).isNull();
  }

  @Test
  void extractEntityIdFromQuestion_nullInputs_returnsNull() {
    assertThat(service.extractEntityIdFromQuestion(null, "cycle")).isNull();
    assertThat(service.extractEntityIdFromQuestion("cycle 5", null)).isNull();
  }

  // ===== expandVagueQuestion tests =====

  @Test
  void expandVagueQuestion_shortCycleQuery_expands() {
    // When
    String expanded = service.expandVagueQuestion("cycle 5", "cycle", 5L);

    // Then
    assertThat(expanded).isEqualTo("What pitches are in cycle 5?");
  }

  @Test
  void expandVagueQuestion_shortPitchQuery_expands() {
    // When
    String expanded = service.expandVagueQuestion("pitch 10", "pitch", 10L);

    // Then
    assertThat(expanded).isEqualTo("Tell me about pitch 10");
  }

  @Test
  void expandVagueQuestion_shortTeamQuery_expands() {
    // When
    String expanded = service.expandVagueQuestion("team 3", "team", 3L);

    // Then
    assertThat(expanded).isEqualTo("What is team 3 working on?");
  }

  @Test
  void expandVagueQuestion_detailedQuestion_unchanged() {
    // Given
    String detailed = "What are the risks associated with the payment feature?";

    // When
    String expanded = service.expandVagueQuestion(detailed, "pitch", 5L);

    // Then
    assertThat(expanded).isEqualTo(detailed);
  }

  @Test
  void expandVagueQuestion_emptyQuestion_returnsEmpty() {
    assertThat(service.expandVagueQuestion("", "cycle", 5L)).isEmpty();
    assertThat(service.expandVagueQuestion(null, "cycle", 5L)).isNull();
  }

  // ===== checkForAmbiguousContext tests =====

  @Test
  void checkForAmbiguousContext_noContextType_returnsNull() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("What is this cycle about?");

    // When
    String result = service.checkForAmbiguousContext(request);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void checkForAmbiguousContext_withContextId_returnsNull() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("What is this cycle about?");
    request.setContextType("cycle");
    request.setContextId(5L);

    // When
    String result = service.checkForAmbiguousContext(request);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void checkForAmbiguousContext_thisCycleAmbiguous_returnsMessage() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("Tell me about this cycle");
    request.setContextType("cycle");

    // When
    String result = service.checkForAmbiguousContext(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("which cycle");
  }

  @Test
  void checkForAmbiguousContext_thisPitchAmbiguous_returnsMessage() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("What is this pitch status?");
    request.setContextType("pitch");

    // When
    String result = service.checkForAmbiguousContext(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("which pitch");
  }

  @Test
  void checkForAmbiguousContext_specificCycleNumber_returnsNull() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("Tell me about cycle 5");
    request.setContextType("cycle");

    // When
    String result = service.checkForAmbiguousContext(request);

    // Then - cycle 5 is specific, not ambiguous
    assertThat(result).isNull();
  }

  // ===== generateClarificationSuggestions tests =====

  @Test
  void generateClarificationSuggestions_cycleContext_returnsCycleSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("cycle");

    // When
    List<String> suggestions = service.generateClarificationSuggestions(request);

    // Then
    assertThat(suggestions).hasSize(3);
    assertThat(suggestions).anyMatch(s -> s.contains("cycle"));
  }

  @Test
  void generateClarificationSuggestions_pitchContext_returnsPitchSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setContextType("pitch");

    // When
    List<String> suggestions = service.generateClarificationSuggestions(request);

    // Then
    assertThat(suggestions).hasSize(3);
    assertThat(suggestions).anyMatch(s -> s.toLowerCase().contains("pitch"));
  }

  @Test
  void generateClarificationSuggestions_noContext_returnsDefaultSuggestions() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();

    // When
    List<String> suggestions = service.generateClarificationSuggestions(request);

    // Then
    assertThat(suggestions).hasSize(3);
  }

  // ===== process tests =====

  @Test
  void process_basicQuestion_returnsResult() {
    // Given
    AskQuestionRequest request = new AskQuestionRequest();
    request.setQuestion("What is cycle 5?");

    // When
    QuestionProcessingService.QuestionProcessingResult result = service.process(request);

    // Then
    assertThat(result.getOriginalQuestion()).isEqualTo("What is cycle 5?");
    assertThat(result.getSubQueries()).contains("What is cycle 5?");
    assertThat(result.isWasDecomposed()).isFalse();
    assertThat(result.getSearchTerms()).isNotEmpty();
  }
}
