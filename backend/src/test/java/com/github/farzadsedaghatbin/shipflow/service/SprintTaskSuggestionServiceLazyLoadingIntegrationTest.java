package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test for a production 500: {@code suggestTasks()} read {@code cycle.getProject()}
 * (a lazy {@code @ManyToOne}) after the repository call's own transaction had already closed,
 * throwing {@link org.hibernate.LazyInitializationException}. Reproducing this requires a real
 * Hibernate session lifecycle with no test-level {@code @Transactional} keeping it artificially
 * open — a Mockito-only unit test (see {@link SprintTaskSuggestionServiceTest}) cannot catch this
 * class of bug because its mocked repository returns an already-fully-built POJO, never a proxy.
 */
@SpringBootTest
@ActiveProfiles("test")
class SprintTaskSuggestionServiceLazyLoadingIntegrationTest {

  @Autowired private SprintTaskSuggestionService sprintTaskSuggestionService;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private CycleRepository cycleRepository;

  @MockBean private ChatLanguageModel chatLanguageModel;

  private static final String VALID_JSON =
      """
      [
        {
          "title": "Build settings API endpoint",
          "description": "Backend and mobile collaborate on a PATCH endpoint and its call site.",
          "sourceContext": "SPRINT",
          "disciplines": ["BACKEND", "MOBILE"]
        }
      ]
      """;

  @Test
  void suggestTasks_doesNotThrowLazyInitializationException_forAPersistedCycle() {
    Project project =
        projectRepository.save(
            Project.builder()
                .name("Lazy Loading Regression Project")
                .projectKey("LLR" + System.nanoTime() % 100000)
                .isActive(true)
                .projectType(ProjectType.SCRUM)
                .createdAt(LocalDateTime.now())
                .build());

    Cycle cycle =
        cycleRepository.save(
            Cycle.builder()
                .project(project)
                .name("Sprint Regression Test")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .phase(CyclePhase.SHAPING_BUILDING)
                .isActive(true)
                .sprintGoal("Verify the lazy-loading fix end to end.")
                .build());

    when(chatLanguageModel.generate(anyString())).thenReturn(VALID_JSON);

    // No @Transactional on this test/class — matches production, where suggestTasks() has no
    // ambient transaction and spring.jpa.open-in-view=false closes the repository's own session
    // before this line runs, so a lazy cycle.getProject() access here would throw exactly as it
    // did in production.
    TaskSuggestionResponseDTO result = sprintTaskSuggestionService.suggestTasks(cycle.getId());

    assertThat(result.getSuggestions()).isNotEmpty();
  }
}
