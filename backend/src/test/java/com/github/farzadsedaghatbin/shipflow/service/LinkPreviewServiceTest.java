package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService.LinkPreview;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Link previews are fetched by crawlers with no login, so what {@link LinkPreviewService} returns is
 * readable by anyone with the URL. These tests pin the contract: title plus general labels, and
 * <strong>never</strong> the free text a person wrote.
 */
@DisplayName("LinkPreviewService")
class LinkPreviewServiceTest {

  /** Deliberately credential-shaped, so a leak would be unmistakable in a failure message. */
  private static final String SECRET = "password=hunter2 customer=Acme internal-sheet-id";

  private TaskRepository taskRepository;
  private PitchRepository pitchRepository;
  private BugReportRepository bugReportRepository;
  private CycleRepository cycleRepository;
  private LinkPreviewService service;

  @BeforeEach
  void setUp() {
    taskRepository = mock(TaskRepository.class);
    pitchRepository = mock(PitchRepository.class);
    bugReportRepository = mock(BugReportRepository.class);
    cycleRepository = mock(CycleRepository.class);
    service = new LinkPreviewService(taskRepository, pitchRepository, bugReportRepository, cycleRepository);
  }

  @Test
  @DisplayName("a task preview is its title plus general labels")
  void taskShowsTitleAndLabels() {
    when(taskRepository.findById(7L)).thenReturn(Optional.of(Task.builder().title("Analytics handoff pack")
        .status(TaskStatus.BACKLOG).priority(TaskPriority.URGENT).build()));

    LinkPreview preview = service.forTask(7L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Analytics handoff pack");
    assertThat(preview.summary()).isEqualTo("Task · Backlog · Urgent");
  }

  @Test
  @DisplayName("a task's description never reaches the preview")
  void taskDescriptionIsNeverExposed() {
    // The previous implementation put the first 160 characters of the description into
    // og:description, readable by any crawler-shaped request that counted up the task ids.
    when(taskRepository.findById(7L)).thenReturn(Optional.of(Task.builder().title("Handoff")
        .description(SECRET).status(TaskStatus.BACKLOG).build()));

    LinkPreview preview = service.forTask(7L).orElseThrow();

    assertThat(preview.title()).doesNotContain("hunter2");
    assertThat(preview.summary()).doesNotContain("hunter2").doesNotContain("Acme");
  }

  @Test
  @DisplayName("a soft-deleted task previews as not found")
  void deletedTaskIsHidden() {
    when(taskRepository.findById(7L)).thenReturn(Optional.of(
        Task.builder().title("Gone").deletedAt(LocalDateTime.now()).build()));

    assertThat(service.forTask(7L)).isEmpty();
  }

  @Test
  @DisplayName("a task that does not exist previews as not found")
  void missingTaskIsEmpty() {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThat(service.forTask(404L)).isEmpty();
  }

  @Test
  @DisplayName("a bug report preview is its title, severity and status — not its description")
  void bugShowsTitleAndLabelsOnly() {
    when(bugReportRepository.findById(3L)).thenReturn(Optional.of(BugReport.builder().title("Login loops")
        .description(SECRET).severity(BugSeverity.TRIVIAL).status(BugStatus.OPEN).build()));

    LinkPreview preview = service.forBug(3L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Login loops");
    assertThat(preview.summary()).isEqualTo("Bug report · Trivial · Open");
    assertThat(preview.summary()).doesNotContain("hunter2");
  }

  @Test
  @DisplayName("a pitch preview is its title and status — not its problem statement or description")
  void pitchShowsTitleAndStatusOnly() {
    when(pitchRepository.findByIdNotDeleted(5L)).thenReturn(Optional.of(Pitch.builder().title("Faster onboarding")
        .problemStatement(SECRET).description(SECRET).status(PitchStatus.IN_PROGRESS).build()));

    LinkPreview preview = service.forPitch(5L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Faster onboarding");
    assertThat(preview.summary()).isEqualTo("Pitch · In Progress");
    assertThat(preview.summary()).doesNotContain("hunter2");
  }

  @Test
  @DisplayName("a pitch is looked up through the not-deleted query, never plain findById")
  void pitchLookupExcludesDeleted() {
    when(pitchRepository.findByIdNotDeleted(5L)).thenReturn(Optional.empty());

    assertThat(service.forPitch(5L)).isEmpty();
    verify(pitchRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("a cycle preview summarises counts and dates")
  void cycleShowsCountsAndDates() {
    Cycle cycle = Cycle.builder().id(12L).name("Cycle 12").startDate(LocalDate.of(2026, 8, 1))
        .endDate(LocalDate.of(2026, 8, 28)).build();
    when(cycleRepository.findById(12L)).thenReturn(Optional.of(cycle));
    when(pitchRepository.countByCycleIdNotDeleted(12L)).thenReturn(4L);

    LinkPreview preview = service.forCycle(12L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Cycle 12");
    assertThat(preview.summary()).isEqualTo("Cycle: 4 pitches · 2026-08-01 – 2026-08-28");
  }

  @Test
  @DisplayName("a cycle counts pitches by query, never through the lazy collection")
  void cycleDoesNotTouchLazyCollection() {
    // Regression: this used to read cycle.getPitches().size(). The collection is LAZY and the code ran
    // outside a session, so it threw LazyInitializationException and every cycle link previewed as a
    // 500. Mimic a detached entity by making the getter blow up the way Hibernate's proxy does.
    Cycle detached = spy(Cycle.builder().id(12L).name("Cycle 12").startDate(LocalDate.of(2026, 8, 1))
        .endDate(LocalDate.of(2026, 8, 28)).build());
    when(detached.getPitches()).thenThrow(new org.hibernate.LazyInitializationException("no Session"));
    when(cycleRepository.findById(12L)).thenReturn(Optional.of(detached));
    when(pitchRepository.countByCycleIdNotDeleted(12L)).thenReturn(1L);

    assertThat(service.forCycle(12L).orElseThrow().summary()).startsWith("Cycle: 1 pitch ");
  }

  @Test
  @DisplayName("enum names become readable labels, and unset values are skipped")
  void labelsAreReadableAndNullSafe() {
    assertThat(LinkPreviewService.label(PitchStatus.IN_PROGRESS)).isEqualTo("In Progress");
    assertThat(LinkPreviewService.label(TaskStatus.BACKLOG)).isEqualTo("Backlog");
    assertThat(LinkPreviewService.label(null)).isNull();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(Task.builder().title("No labels").build()));
    assertThat(service.forTask(1L).orElseThrow().summary()).isEqualTo("Task");
  }
}
