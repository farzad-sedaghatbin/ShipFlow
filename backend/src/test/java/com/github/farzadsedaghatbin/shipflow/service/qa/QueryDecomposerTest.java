package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryDecomposerTest {

  @Mock private ChatLanguageModel chatLanguageModel;

  private QueryDecomposer queryDecomposer;

  @BeforeEach
  void setUp() {
    queryDecomposer = new QueryDecomposer(chatLanguageModel);
  }

  @Test
  void shouldDecompose_simpleQuestion_returnsFalse() {
    // Given
    String simpleQuestion = "What are the payment risks?";

    // When
    boolean result = queryDecomposer.shouldDecompose(simpleQuestion);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void shouldDecompose_tooShortQuestion_returnsFalse() {
    // Given
    String shortQuestion = "Compare?";

    // When
    boolean result = queryDecomposer.shouldDecompose(shortQuestion);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void shouldDecompose_compareQuestion_returnsTrue() {
    // Given
    String compareQuestion = "Compare payment feature versus checkout redesign";

    // When
    boolean result = queryDecomposer.shouldDecompose(compareQuestion);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void shouldDecompose_vsQuestion_returnsTrue() {
    // Given
    String vsQuestion = "What are payment risks vs checkout risks?";

    // When
    boolean result = queryDecomposer.shouldDecompose(vsQuestion);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void shouldDecompose_multipleQuestions_returnsTrue() {
    // Given
    String multiQuestion = "What are the risks? Which team is working on it?";

    // When
    boolean result = queryDecomposer.shouldDecompose(multiQuestion);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void decompose_simpleQuestion_returnsOriginal() {
    // Given
    String simpleQuestion = "What are the payment risks?";

    // When
    List<String> result = queryDecomposer.decompose(simpleQuestion);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(simpleQuestion);
  }

  @Test
  void decompose_complexQuestion_returnsSubQueries() {
    // Given
    String complexQuestion = "Compare payment feature risks versus checkout redesign risks";
    String llmResponse =
        "1. What are the payment feature risks?\n2. What are the checkout redesign risks?";
    when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

    // When
    List<String> result = queryDecomposer.decompose(complexQuestion);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isEqualTo("What are the payment feature risks?");
    assertThat(result.get(1)).isEqualTo("What are the checkout redesign risks?");
  }

  @Test
  void decompose_llmReturnsNull_returnsOriginal() {
    // Given
    String question = "Compare payment vs checkout";
    when(chatLanguageModel.generate(anyString())).thenReturn(null);

    // When
    List<String> result = queryDecomposer.decompose(question);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(question);
  }

  @Test
  void decompose_llmThrowsException_returnsOriginal() {
    // Given
    String question = "Compare payment vs checkout";
    when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("LLM error"));

    // When
    List<String> result = queryDecomposer.decompose(question);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(question);
  }

  @Test
  void decompose_llmReturnsEmptyList_returnsOriginal() {
    // Given
    String question = "Compare payment vs checkout";
    String llmResponse = ""; // Empty response
    when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

    // When
    List<String> result = queryDecomposer.decompose(question);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(question);
  }

  @Test
  void decompose_multipleNumberedQuestions_extractsAll() {
    // Given
    String complexQuestion = "Compare features and analyze risks and identify teams";
    String llmResponse =
        """
            1. What are the features to compare?
            2. What are the associated risks?
            3. Which teams are involved?
            """;
    when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

    // When
    List<String> result = queryDecomposer.decompose(complexQuestion);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0)).isEqualTo("What are the features to compare?");
    assertThat(result.get(1)).isEqualTo("What are the associated risks?");
    assertThat(result.get(2)).isEqualTo("Which teams are involved?");
  }

  @Test
  void decomposeWithMetadata_simpleQuestion_returnsNotDecomposed() {
    // Given
    String simpleQuestion = "What are the payment risks?";

    // When
    QueryDecomposer.DecompositionResult result =
        queryDecomposer.decomposeWithMetadata(simpleQuestion);

    // Then
    assertThat(result.wasDecomposed()).isFalse();
    assertThat(result.getSubQueries()).hasSize(1);
    assertThat(result.getSubQueries().get(0)).isEqualTo(simpleQuestion);
    assertThat(result.getSubQueryCount()).isEqualTo(1);
  }

  @Test
  void decomposeWithMetadata_complexQuestion_returnsDecomposed() {
    // Given
    String complexQuestion = "Compare payment vs checkout risks";
    String llmResponse = "1. What are payment risks?\n2. What are checkout risks?";
    when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

    // When
    QueryDecomposer.DecompositionResult result =
        queryDecomposer.decomposeWithMetadata(complexQuestion);

    // Then
    assertThat(result.wasDecomposed()).isTrue();
    assertThat(result.getSubQueries()).hasSize(2);
    assertThat(result.getSubQueryCount()).isEqualTo(2);
  }
}
