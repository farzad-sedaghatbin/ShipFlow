package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StandupDigestBuilder")
class StandupDigestBuilderTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 9, 0);

  @Mock private TaskRepository taskRepository;
  @Mock private BugReportRepository bugReportRepository;

  private StandupDigestBuilder builder;
  private Project project;
  private final AgentLoopConfig config = AgentLoopConfig.parse("{\"lookbackHours\":24}", new ObjectMapper());

  @BeforeEach
  void setUp() {
    builder = new StandupDigestBuilder(taskRepository, bugReportRepository);
    project = new Project();
    project.setId(5L);
    project.setName("Payments");
    when(taskRepository.findCompletedSince(any(), any(), any())).thenReturn(List.of());
    when(taskRepository.findByProjectAndStatusIn(any(), any())).thenReturn(List.of());
    when(taskRepository.findOverdueByProject(any(), any(), any())).thenReturn(List.of());
    when(bugReportRepository.findCreatedSince(any(), any())).thenReturn(List.of());
  }

  private static Task task(String title, String assignee, TaskStatus status) {
    Task t = new Task();
    t.setTitle(title);
    t.setStatus(status);
    if (assignee != null) {
      t.setAssignee(Person.builder().name(assignee).build());
    }
    return t;
  }

  @Test
  @DisplayName("Lists what finished, is in progress, blocked, overdue and newly reported — all from the database")
  void fullDigest() {
    when(taskRepository.findCompletedSince(eq(5L), eq(TaskStatus.DONE), eq(NOW.minusHours(24))))
        .thenReturn(List.of(task("Fix login", "Sara", TaskStatus.DONE)));
    when(taskRepository.findByProjectAndStatusIn(5L, List.of(TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW)))
        .thenReturn(List.of(task("Write docs", "Ali", TaskStatus.IN_PROGRESS), task("Review PR", null, TaskStatus.IN_REVIEW)));
    when(taskRepository.findByProjectAndStatusIn(5L, List.of(TaskStatus.BLOCKED)))
        .thenReturn(List.of(task("Deploy", "Ali", TaskStatus.BLOCKED)));
    Task late = task("Pay invoice", "Sara", TaskStatus.TODO);
    late.setDueDate(LocalDate.of(2026, 11, 1));
    when(taskRepository.findOverdueByProject(eq(5L), eq(NOW.toLocalDate()), any())).thenReturn(List.of(late));
    BugReport bug = new BugReport();
    bug.setBugKey("BUG-12");
    bug.setTitle("Crash on save");
    when(bugReportRepository.findCreatedSince(eq(5L), eq(NOW.minusHours(24)))).thenReturn(List.of(bug));

    AgentLoopReport r = builder.build(project, config, NOW);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.title()).isEqualTo("Standup digest — Payments");
    assertThat(r.facts())
        .contains("Last 24 hours in Payments")
        .contains("✅ Finished (1)").contains("Fix login — Sara")
        .contains("🔄 In progress (2)").contains("Review PR — unassigned (in review)")
        .contains("⛔ Blocked (1)").contains("Deploy — Ali")
        .contains("⏰ Overdue (1)").contains("Pay invoice — Sara — due 2026-11-01")
        .contains("🐞 New bugs (1)").contains("BUG-12 Crash on save");
    assertThat(r.entityType()).isEqualTo("PROJECT");
    assertThat(r.entityId()).isEqualTo(5L);
  }

  @Test
  @DisplayName("A quiet project yields a report with no findings, so nothing is posted")
  void quiet() {
    AgentLoopReport r = builder.build(project, config, NOW);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).contains("Nothing moved and nothing is stuck.");
  }

  @Test
  @DisplayName("Long lists are capped with an 'and N more' line")
  void capsLongLists() {
    List<Task> many = java.util.stream.IntStream.range(0, 12)
        .mapToObj(i -> task("Task " + i, "Sara", TaskStatus.DONE)).toList();
    when(taskRepository.findCompletedSince(any(), any(), any())).thenReturn(many);

    String facts = builder.build(project, config, NOW).facts();

    assertThat(facts).contains("✅ Finished (12)").contains("Task 7").doesNotContain("Task 8").contains("…and 4 more");
  }

  @Test
  @DisplayName("Titles written by people can't smuggle channel pings or links into the digest")
  void titlesAreCleaned() {
    when(taskRepository.findCompletedSince(any(), any(), any()))
        .thenReturn(List.of(task("<!channel> read <https://evil|this>", "@here", TaskStatus.DONE)));

    String facts = builder.build(project, config, NOW).facts();

    assertThat(facts).doesNotContain("<").doesNotContain(">").doesNotContain("@here").doesNotContain("!channel");
  }

  @Test
  @DisplayName("The look-back window comes from the settings")
  void lookback() {
    AgentLoopConfig week = AgentLoopConfig.parse("{\"lookbackHours\":168}", new ObjectMapper());

    builder.build(project, week, NOW);

    verify(taskRepository).findCompletedSince(eq(5L), eq(TaskStatus.DONE), eq(NOW.minusHours(168)));
  }
}
