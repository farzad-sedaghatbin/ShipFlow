package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.service.mcp.FigmaMcpProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PitchTaskSuggestionService}.
 *
 * <p>No Spring context needed — all collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PitchTaskSuggestionService Tests")
class PitchTaskSuggestionServiceTest {

  @Mock private ChatLanguageModel chatLanguageModel;
  @Mock private PitchRepository pitchRepository;
  @Mock private OrganizationSettingsService settingsService;
  @Mock private FigmaMcpProvider figmaMcpProvider;

  private PitchTaskSuggestionService service;

  private static final String VALID_JSON =
      """
      [
        {
          "title": "Build settings API endpoint",
          "description": "Backend and mobile collaborate on a PATCH endpoint and its call site.",
          "estimateHours": 8,
          "sourceContext": "PITCH",
          "disciplines": ["BACKEND", "MOBILE", "QA"]
        },
        {
          "title": "Write migration script",
          "description": "Pure backend migration with no UI surface.",
          "sourceContext": "PITCH",
          "disciplines": ["BACKEND"]
        }
      ]
      """;

  private Pitch samplePitch(String wireframeLinks) {
    Pitch pitch = new Pitch();
    pitch.setId(1L);
    pitch.setTitle("Faster Onboarding");
    pitch.setProblemStatement("New users bounce during setup.");
    pitch.setSolution("Streamline the onboarding wizard.");
    pitch.setAppetiteDays(14);
    pitch.setRabbitHoles("Avoid rewriting auth.");
    pitch.setRisks("Might need design review.");
    pitch.setNoGos("No SSO changes.");
    pitch.setWireframeLinks(wireframeLinks);
    return pitch;
  }

  @BeforeEach
  void setUp() {
    service = new PitchTaskSuggestionService(
        chatLanguageModel, new ObjectMapper(), pitchRepository, settingsService, figmaMcpProvider);
  }

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
      PitchTaskSuggestionService serviceWithoutModel = new PitchTaskSuggestionService(
          null, new ObjectMapper(), pitchRepository, settingsService, figmaMcpProvider);
      assertThat(serviceWithoutModel.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("suggestTasks() — pitch-only (no Figma)")
  class PitchOnlyTests {

    @Test
    @DisplayName("returns parsed suggestions and figmaContextUsed=false when no wireframe links")
    void noWireframeLinks_figmaContextUnused() {
      Pitch pitch = samplePitch(null);
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.isFigmaContextUsed()).isFalse();
      assertThat(result.getSuggestions()).hasSize(2);
      assertThat(result.getSuggestions().get(0).getTitle()).isEqualTo("Build settings API endpoint");
      assertThat(result.getSuggestions().get(0).getDisciplines()).contains(
          com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline.BACKEND,
          com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline.MOBILE,
          com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline.QA);
      assertThat(result.getSuggestions().get(1).getDisciplines()).containsExactly(
          com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline.BACKEND);
    }

    @Test
    @DisplayName("prompt contains pitch context fields and omits DESIGN CONTEXT block")
    void promptContainsPitchFields() {
      Pitch pitch = samplePitch(null);
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      service.suggestTasks(1L);

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());

      String prompt = promptCaptor.getValue();
      assertThat(prompt).contains("Faster Onboarding");
      assertThat(prompt).contains("New users bounce during setup.");
      assertThat(prompt).contains("Streamline the onboarding wizard.");
      assertThat(prompt).doesNotContain("DESIGN CONTEXT");
    }

    @Test
    @DisplayName("degrades gracefully when wireframe links present but no Figma URL parses")
    void wireframeLinksWithoutFigmaUrl_degradesGracefully() {
      Pitch pitch = samplePitch("see attached mockup.png");
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      when(figmaMcpProvider.extractFigmaFileReferences("see attached mockup.png"))
          .thenReturn(Collections.emptyList());
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.isFigmaContextUsed()).isFalse();
    }

    @Test
    @DisplayName("degrades gracefully when Figma URL parses but org token is not configured")
    void figmaUrlWithoutToken_degradesGracefully() {
      Pitch pitch = samplePitch("https://figma.com/file/abc123/Design");
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      FigmaMcpProvider.FigmaFileReference ref = FigmaMcpProvider.FigmaFileReference.builder()
          .fileKey("abc123").nodeId(null).originalUrl("https://figma.com/file/abc123/Design").build();
      when(figmaMcpProvider.extractFigmaFileReferences(anyString())).thenReturn(List.of(ref));
      when(settingsService.getFigmaAccessToken()).thenReturn(null);
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.isFigmaContextUsed()).isFalse();
    }
  }

  @Nested
  @DisplayName("suggestTasks() — with Figma context")
  class FigmaAugmentedTests {

    @Test
    @DisplayName("figmaContextUsed=true and prompt includes DESIGN CONTEXT when Figma content is available")
    void figmaContentAvailable_augmentsPrompt() {
      Pitch pitch = samplePitch("https://figma.com/file/abc123/Design");
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      FigmaMcpProvider.FigmaFileReference ref = FigmaMcpProvider.FigmaFileReference.builder()
          .fileKey("abc123").nodeId(null).originalUrl("https://figma.com/file/abc123/Design").build();
      when(figmaMcpProvider.extractFigmaFileReferences(anyString())).thenReturn(List.of(ref));
      when(settingsService.getFigmaAccessToken()).thenReturn("figma-token");
      FigmaMcpProvider.FigmaDesignContext context = FigmaMcpProvider.FigmaDesignContext.builder()
          .fileName("Onboarding Wizard")
          .components(List.of("OnboardingCard", "ProgressBar"))
          .build();
      when(figmaMcpProvider.getDesignContext(ref, "figma-token")).thenReturn(context);
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.isFigmaContextUsed()).isTrue();

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());
      assertThat(promptCaptor.getValue()).contains("DESIGN CONTEXT");
      assertThat(promptCaptor.getValue()).contains("Onboarding Wizard");
      assertThat(promptCaptor.getValue()).contains("OnboardingCard");
    }

    @Test
    @DisplayName("degrades gracefully when Figma context has no meaningful content")
    void figmaContextEmpty_degradesGracefully() {
      Pitch pitch = samplePitch("https://figma.com/file/abc123/Design");
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      FigmaMcpProvider.FigmaFileReference ref = FigmaMcpProvider.FigmaFileReference.builder()
          .fileKey("abc123").nodeId(null).originalUrl("https://figma.com/file/abc123/Design").build();
      when(figmaMcpProvider.extractFigmaFileReferences(anyString())).thenReturn(List.of(ref));
      when(settingsService.getFigmaAccessToken()).thenReturn("figma-token");
      when(figmaMcpProvider.getDesignContext(ref, "figma-token"))
          .thenReturn(FigmaMcpProvider.FigmaDesignContext.builder().build());
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.isFigmaContextUsed()).isFalse();
    }
  }

  @Nested
  @DisplayName("suggestTasks() — error cases")
  class ErrorTests {

    @Test
    @DisplayName("throws IllegalStateException when ChatLanguageModel is null")
    void throwsWhenModelIsNull() {
      PitchTaskSuggestionService serviceWithoutModel = new PitchTaskSuggestionService(
          null, new ObjectMapper(), pitchRepository, settingsService, figmaMcpProvider);

      assertThatThrownBy(() -> serviceWithoutModel.suggestTasks(1L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when pitch doesn't exist")
    void throwsWhenPitchNotFound() {
      when(pitchRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.suggestTasks(99L))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when LLM returns non-JSON text")
    void throwsWhenLlmReturnsNonJson() {
      Pitch pitch = samplePitch(null);
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      when(chatLanguageModel.generate(anyString())).thenReturn("Sorry, I cannot help with that.");

      assertThatThrownBy(() -> service.suggestTasks(1L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("drops malformed suggestions missing disciplines instead of surfacing them")
    void dropsMalformedSuggestions() {
      Pitch pitch = samplePitch(null);
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
      String malformedJson =
          """
          [
            { "title": "Missing disciplines", "sourceContext": "PITCH", "disciplines": [] },
            { "title": "Valid task", "sourceContext": "PITCH", "disciplines": ["BACKEND"] }
          ]
          """;
      when(chatLanguageModel.generate(anyString())).thenReturn(malformedJson);

      TaskSuggestionResponseDTO result = service.suggestTasks(1L);

      assertThat(result.getSuggestions()).hasSize(1);
      assertThat(result.getSuggestions().get(0).getTitle()).isEqualTo("Valid task");
    }
  }
}
