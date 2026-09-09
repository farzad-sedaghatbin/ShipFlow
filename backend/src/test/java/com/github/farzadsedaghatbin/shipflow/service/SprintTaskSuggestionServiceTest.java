package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SuggestionSource;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDate;
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
 * Unit tests for {@link SprintTaskSuggestionService}.
 *
 * <p>No Spring context needed — all collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintTaskSuggestionService Tests")
class SprintTaskSuggestionServiceTest {

  @Mock private ChatLanguageModel chatLanguageModel;
  @Mock private CycleRepository cycleRepository;
  @Mock private TaskRepository taskRepository;

  private SprintTaskSuggestionService service;

  private static final String VALID_JSON =
      """
      [
        {
          "title": "Build settings API endpoint",
          "description": "Backend and mobile collaborate on a PATCH endpoint and its call site.",
          "estimateHours": 8,
          "sourceContext": "SPRINT",
          "disciplines": ["BACKEND", "MOBILE", "QA"]
        },
        {
          "title": "Write migration script",
          "description": "Pure backend migration with no UI surface.",
          "sourceContext": "SPRINT",
          "disciplines": ["BACKEND"]
        }
      ]
      """;

  private Cycle sampleCycle(String sprintGoal) {
    Project project = Project.builder().id(1L).name("Sample Project").build();
    return Cycle.builder()
        .id(10L)
        .project(project)
        .name("Sprint 12")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(2))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true)
        .sprintGoal(sprintGoal)
        .build();
  }

  @BeforeEach
  void setUp() {
    service = new SprintTaskSuggestionService(chatLanguageModel, new ObjectMapper(), cycleRepository,
        taskRepository);
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
      SprintTaskSuggestionService serviceWithoutModel =
          new SprintTaskSuggestionService(null, new ObjectMapper(), cycleRepository, taskRepository);
      assertThat(serviceWithoutModel.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("suggestTasks() — with sprint goal")
  class WithSprintGoalTests {

    @Test
    @DisplayName("returns parsed suggestions grounded in the sprint goal")
    void returnsParsedSuggestions() {
      Cycle cycle = sampleCycle("Ship the new onboarding wizard end to end.");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(10L);

      assertThat(result.isFigmaContextUsed()).isFalse();
      assertThat(result.getSuggestions()).hasSize(2);
      assertThat(result.getSuggestions().get(0).getTitle()).isEqualTo("Build settings API endpoint");
      assertThat(result.getSuggestions().get(1).getDisciplines()).containsExactly(
          com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline.BACKEND);
    }

    @Test
    @DisplayName("prompt contains sprint goal, name, project, and existing task titles")
    void promptContainsSprintFields() {
      Cycle cycle = sampleCycle("Ship the new onboarding wizard end to end.");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));

      Task existing = new Task();
      existing.setTitle("Existing onboarding task");
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(List.of(existing));
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      service.suggestTasks(10L);

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());

      String prompt = promptCaptor.getValue();
      assertThat(prompt).contains("Sprint 12");
      assertThat(prompt).contains("Ship the new onboarding wizard end to end.");
      assertThat(prompt).contains("Sample Project");
      assertThat(prompt).contains("Existing onboarding task");
      assertThat(prompt).doesNotContain("Appetite");
      assertThat(prompt).doesNotContain("Rabbit Holes");
      assertThat(prompt).doesNotContain("No-Gos");
      assertThat(prompt).doesNotContain("Figma");
    }
  }

  @Nested
  @DisplayName("suggestTasks() — no sprint goal set")
  class NoSprintGoalTests {

    @Test
    @DisplayName("degrades gracefully and instructs conservative inference from name/backlog")
    void noSprintGoal_promptInstructsConservativeInference() {
      Cycle cycle = sampleCycle(null);
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      TaskSuggestionResponseDTO result = service.suggestTasks(10L);

      assertThat(result.getSuggestions()).hasSize(2);

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());
      assertThat(promptCaptor.getValue()).contains("none set")
          .contains("infer scope conservatively from the sprint name and existing backlog");
    }

    @Test
    @DisplayName("handles blank sprint goal the same as null")
    void blankSprintGoal_treatedAsUnset() {
      Cycle cycle = sampleCycle("   ");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

      service.suggestTasks(10L);

      ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
      verify(chatLanguageModel).generate(promptCaptor.capture());
      assertThat(promptCaptor.getValue()).contains("none set");
    }
  }

  @Nested
  @DisplayName("suggestTasks() — sourceContext forcing")
  class SourceContextForcingTests {

    @Test
    @DisplayName("forces sourceContext to SPRINT even if the LLM claims otherwise")
    void forcesSourceContextToSprint() {
      Cycle cycle = sampleCycle("Sprint goal");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      String jsonWithWrongSourceContext =
          """
          [
            {
              "title": "Task claiming PITCH",
              "description": "The LLM hallucinated a pitch-flavored sourceContext.",
              "sourceContext": "PITCH",
              "disciplines": ["BACKEND"]
            },
            {
              "title": "Task claiming PITCH_DESIGN",
              "description": "The LLM hallucinated a design-flavored sourceContext.",
              "sourceContext": "PITCH_DESIGN",
              "disciplines": ["DESIGN"]
            }
          ]
          """;
      when(chatLanguageModel.generate(anyString())).thenReturn(jsonWithWrongSourceContext);

      TaskSuggestionResponseDTO result = service.suggestTasks(10L);

      assertThat(result.getSuggestions()).hasSize(2);
      assertThat(result.getSuggestions()).allSatisfy(
          s -> assertThat(s.getSourceContext()).isEqualTo(SuggestionSource.SPRINT));
    }
  }

  @Nested
  @DisplayName("suggestTasks() — error cases")
  class ErrorTests {

    @Test
    @DisplayName("throws IllegalStateException when ChatLanguageModel is null")
    void throwsWhenModelIsNull() {
      SprintTaskSuggestionService serviceWithoutModel =
          new SprintTaskSuggestionService(null, new ObjectMapper(), cycleRepository, taskRepository);

      assertThatThrownBy(() -> serviceWithoutModel.suggestTasks(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when cycle doesn't exist")
    void throwsWhenCycleNotFound() {
      when(cycleRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.suggestTasks(99L))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when LLM returns non-JSON text")
    void throwsWhenLlmReturnsNonJson() {
      Cycle cycle = sampleCycle("Sprint goal");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      when(chatLanguageModel.generate(anyString())).thenReturn("Sorry, I cannot help with that.");

      assertThatThrownBy(() -> service.suggestTasks(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("drops malformed suggestions missing disciplines instead of surfacing them")
    void dropsMalformedSuggestions() {
      Cycle cycle = sampleCycle("Sprint goal");
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
      when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(Collections.emptyList());
      String malformedJson =
          """
          [
            { "title": "Missing disciplines", "sourceContext": "SPRINT", "disciplines": [] },
            { "title": "Valid task", "sourceContext": "SPRINT", "disciplines": ["BACKEND"] }
          ]
          """;
      when(chatLanguageModel.generate(anyString())).thenReturn(malformedJson);

      TaskSuggestionResponseDTO result = service.suggestTasks(10L);

      assertThat(result.getSuggestions()).hasSize(1);
      assertThat(result.getSuggestions().get(0).getTitle()).isEqualTo("Valid task");
    }
  }
}
