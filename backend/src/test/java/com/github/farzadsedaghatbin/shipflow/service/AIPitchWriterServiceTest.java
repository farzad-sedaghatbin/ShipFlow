package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.PitchWriterRequestDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.PitchWriterResponseDTO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AIPitchWriterService}.
 *
 * <p>No Spring context needed — all collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AIPitchWriterService Tests")
class AIPitchWriterServiceTest {

  @Mock private ChatLanguageModel chatLanguageModel;

  private AIPitchWriterService service;

  private static final String VALID_JSON =
      """
      {
        "title": "Inline Pitch Editing",
        "problemStatement": "Users must navigate to a separate edit page to rename a pitch.",
        "solution": "Allow users to click the pitch title to edit it directly on the detail page.",
        "appetiteDays": 14,
        "rabbitHoles": "- Do not implement collaborative real-time editing\\n- Avoid complex undo/redo logic",
        "noGos": "- No rich-text formatting in the title\\n- No batch rename across pitches",
        "risks": "- Optimistic UI updates could desync if network fails"
      }
      """;

  @BeforeEach
  void setUp() {
    service = new AIPitchWriterService(chatLanguageModel, new ObjectMapper());
  }

  // ── isAvailable ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("isAvailable()")
  class IsAvailableTests {

    @Test
    @DisplayName("returns true when ChatLanguageModel is injected")
    void returnsTrue_whenModelPresent() {
      assertThat(service.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("returns false when ChatLanguageModel is null (not configured)")
    void returnsFalse_whenModelAbsent() {
      AIPitchWriterService serviceWithoutModel =
          new AIPitchWriterService(null, new ObjectMapper());
      assertThat(serviceWithoutModel.isAvailable()).isFalse();
    }
  }

  // ── generatePitch — happy path ───────────────────────────────────────────────

  @Nested
  @DisplayName("generatePitch() — happy path")
  class GeneratePitchHappyPathTests {

    @Test
    @DisplayName("parses valid JSON response into DTO")
    void parsesValidJsonResponse() {
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      PitchWriterRequestDTO request =
          PitchWriterRequestDTO.builder()
              .problemDescription("Users struggle to rename pitches quickly")
              .build();

      PitchWriterResponseDTO result = service.generatePitch(request);

      assertThat(result.getTitle()).isEqualTo("Inline Pitch Editing");
      assertThat(result.getProblemStatement()).contains("separate edit page");
      assertThat(result.getSolution()).contains("click the pitch title");
      assertThat(result.getAppetiteDays()).isEqualTo(14);
      assertThat(result.getRabbitHoles()).contains("collaborative real-time editing");
      assertThat(result.getNoGos()).contains("batch rename");
      assertThat(result.getRisks()).contains("Optimistic UI");
    }

    @Test
    @DisplayName("prompt contains the problem description")
    void promptContainsProblemDescription() {
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      String problem = "Team leads cannot see which pitches are blocked";
      service.generatePitch(
          PitchWriterRequestDTO.builder().problemDescription(problem).build());

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());

      assertThat(promptCaptor.getValue()).contains(problem);
    }

    @Test
    @DisplayName("prompt includes appetiteHint when provided")
    void promptIncludesAppetiteHint() {
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      service.generatePitch(
          PitchWriterRequestDTO.builder()
              .problemDescription("Notification overload in dashboard")
              .appetiteHint(28)
              .build());

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());

      assertThat(promptCaptor.getValue()).contains("28");
    }

    @Test
    @DisplayName("prompt includes projectContext when provided")
    void promptIncludesProjectContext() {
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      service.generatePitch(
          PitchWriterRequestDTO.builder()
              .problemDescription("Slow onboarding flow")
              .projectContext("B2B SaaS, 5-person team")
              .build());

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());

      assertThat(promptCaptor.getValue()).contains("B2B SaaS, 5-person team");
    }
  }

  // ── stripMarkdownFences ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("stripMarkdownFences()")
  class StripMarkdownFencesTests {

    @Test
    @DisplayName("passes through plain JSON unchanged")
    void plainJson_unchanged() {
      String plain = "{ \"title\": \"Test\" }";
      assertThat(AIPitchWriterService.stripMarkdownFences(plain)).isEqualTo(plain);
    }

    @Test
    @DisplayName("strips ```json ... ``` fences")
    void stripsJsonFences() {
      String fenced = "```json\n{ \"title\": \"Test\" }\n```";
      assertThat(AIPitchWriterService.stripMarkdownFences(fenced))
          .isEqualTo("{ \"title\": \"Test\" }");
    }

    @Test
    @DisplayName("strips ``` ... ``` fences (no language tag)")
    void stripsPlainFences() {
      String fenced = "```\n{ \"title\": \"Test\" }\n```";
      assertThat(AIPitchWriterService.stripMarkdownFences(fenced))
          .isEqualTo("{ \"title\": \"Test\" }");
    }

    @Test
    @DisplayName("LLM response wrapped in fences is still parsed correctly")
    void fencedResponseIsParsedCorrectly() {
      String fencedJson = "```json\n" + VALID_JSON + "\n```";
      when(chatLanguageModel.generate(anyString())).thenReturn(fencedJson);

      PitchWriterResponseDTO result =
          service.generatePitch(
              PitchWriterRequestDTO.builder()
                  .problemDescription("Pitch editing is cumbersome")
                  .build());

      assertThat(result.getTitle()).isEqualTo("Inline Pitch Editing");
      assertThat(result.getAppetiteDays()).isEqualTo(14);
    }

    @Test
    @DisplayName("handles null response gracefully")
    void handlesNullResponse() {
      assertThat(AIPitchWriterService.stripMarkdownFences(null)).isEmpty();
    }
  }

  // ── generatePitch — error cases ───────────────────────────────────────────────

  @Nested
  @DisplayName("generatePitch() — error cases")
  class GeneratePitchErrorTests {

    @Test
    @DisplayName("throws IllegalStateException when ChatLanguageModel is null")
    void throwsWhenModelIsNull() {
      AIPitchWriterService serviceWithoutModel =
          new AIPitchWriterService(null, new ObjectMapper());

      assertThatThrownBy(
              () ->
                  serviceWithoutModel.generatePitch(
                      PitchWriterRequestDTO.builder()
                          .problemDescription("Some problem")
                          .build()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("throws IllegalStateException when LLM returns non-JSON text")
    void throwsWhenLlmReturnsNonJson() {
      when(chatLanguageModel.generate(anyString()))
          .thenReturn("Sorry, I cannot help with that.");

      assertThatThrownBy(
              () ->
                  service.generatePitch(
                      PitchWriterRequestDTO.builder()
                          .problemDescription("Some problem")
                          .build()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("could not be parsed");
    }
  }
}
