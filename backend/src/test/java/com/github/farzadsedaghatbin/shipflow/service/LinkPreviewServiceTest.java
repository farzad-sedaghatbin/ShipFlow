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
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService.LinkPreview;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
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
  private InitiativeRepository initiativeRepository;
  private WikiSpaceRepository wikiSpaceRepository;
  private WikiPageRepository wikiPageRepository;
  private WikiSpacePermissionRepository wikiSpacePermissionRepository;
  private ProjectRepository projectRepository;
  private LinkPreviewService service;

  @BeforeEach
  void setUp() {
    taskRepository = mock(TaskRepository.class);
    pitchRepository = mock(PitchRepository.class);
    bugReportRepository = mock(BugReportRepository.class);
    cycleRepository = mock(CycleRepository.class);
    initiativeRepository = mock(InitiativeRepository.class);
    wikiSpaceRepository = mock(WikiSpaceRepository.class);
    wikiPageRepository = mock(WikiPageRepository.class);
    wikiSpacePermissionRepository = mock(WikiSpacePermissionRepository.class);
    projectRepository = mock(ProjectRepository.class);
    service = new LinkPreviewService(taskRepository, pitchRepository, bugReportRepository, cycleRepository,
        initiativeRepository, wikiSpaceRepository, wikiPageRepository, wikiSpacePermissionRepository,
        projectRepository);
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

    LinkPreview preview = service.forBug("3").orElseThrow();

    assertThat(preview.title()).isEqualTo("Login loops");
    assertThat(preview.summary()).isEqualTo("Bug report · Trivial · Open");
    assertThat(preview.summary()).doesNotContain("hunter2");
  }

  @Test
  @DisplayName("a bug is found by its key — the URL the bug page settles on — as well as by id")
  void bugResolvesByKey() {
    when(bugReportRepository.findByBugKey("BUG-150")).thenReturn(Optional.of(BugReport.builder()
        .bugKey("BUG-150").title("Login loops").severity(BugSeverity.TRIVIAL).status(BugStatus.OPEN).build()));

    assertThat(service.forBug("BUG-150").orElseThrow().title()).isEqualTo("Login loops");
    verify(bugReportRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("a digit string too long for an id is treated as an unknown key, not an error")
  void overlongNumericBugRefIsNotFound() {
    assertThat(service.forBug("9".repeat(40))).isEmpty();
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
  @DisplayName("an initiative preview is its name and status — not its description")
  void initiativeShowsNameAndStatusOnly() {
    when(initiativeRepository.findByIdNotDeleted(4L)).thenReturn(Optional.of(Initiative.builder().name("Q4 growth")
        .description(SECRET).status(InitiativeStatus.PLANNED).build()));

    LinkPreview preview = service.forInitiative(4L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Q4 growth");
    assertThat(preview.summary()).isEqualTo("Initiative · Planned");
  }

  @Test
  @DisplayName("the roadmap has no id, so it gets a fixed named card")
  void roadmapIsAFixedCard() {
    assertThat(service.forRoadmap().title()).isEqualTo("Roadmap");
  }

  @Test
  @DisplayName("a wiki space preview is its name and page count — not its description")
  void wikiSpaceShowsNameAndPageCount() {
    when(wikiSpaceRepository.findById(1L)).thenReturn(Optional.of(space(1L)));
    when(wikiPageRepository.countBySpaceIdAndDeletedAtIsNull(1L)).thenReturn(3L);

    LinkPreview preview = service.forWikiSpace(1L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Engineering");
    assertThat(preview.summary()).isEqualTo("Wiki space · 3 pages");
  }

  @Test
  @DisplayName("a wiki page preview is its title and space — never its content")
  void wikiPageShowsTitleAndSpaceOnly() {
    when(wikiSpaceRepository.findById(1L)).thenReturn(Optional.of(space(1L)));
    when(wikiPageRepository.findById(8L)).thenReturn(Optional.of(WikiPage.builder().id(8L).spaceId(1L)
        .title("On-call runbook").content(SECRET).contentText(SECRET).build()));

    LinkPreview preview = service.forWikiPage(1L, 8L).orElseThrow();

    assertThat(preview.title()).isEqualTo("On-call runbook");
    assertThat(preview.summary()).isEqualTo("Wiki page · Engineering");
  }

  @Test
  @DisplayName("a private wiki space hides its name and its pages' titles")
  void restrictedWikiHidesNames() {
    when(wikiSpaceRepository.findById(1L)).thenReturn(Optional.of(space(1L)));
    when(wikiSpacePermissionRepository.existsBySpaceId(1L)).thenReturn(true);
    when(wikiPageRepository.findById(8L)).thenReturn(Optional.of(WikiPage.builder().id(8L).spaceId(1L)
        .title("Salary bands").build()));

    assertThat(service.forWikiSpace(1L).orElseThrow()).isEqualTo(new LinkPreview("Wiki space", "Wiki space · Private"));
    assertThat(service.forWikiPage(1L, 8L).orElseThrow()).isEqualTo(new LinkPreview("Wiki page", "Wiki page · Private"));
  }

  @Test
  @DisplayName("a wiki page from another space, or a deleted one, previews as not found")
  void wikiPageMustBeLiveAndInTheSpace() {
    when(wikiSpaceRepository.findById(1L)).thenReturn(Optional.of(space(1L)));
    when(wikiPageRepository.findById(8L)).thenReturn(Optional.of(WikiPage.builder().id(8L).spaceId(2L)
        .title("Elsewhere").build()));
    when(wikiPageRepository.findById(9L)).thenReturn(Optional.of(WikiPage.builder().id(9L).spaceId(1L)
        .title("Gone").deletedAt(OffsetDateTime.now()).build()));

    assertThat(service.forWikiPage(1L, 8L)).isEmpty();
    assertThat(service.forWikiPage(1L, 9L)).isEmpty();
  }

  @Test
  @DisplayName("a deleted wiki space previews as not found, and so do its pages")
  void deletedWikiSpaceIsHidden() {
    WikiSpace deleted = space(1L);
    deleted.setDeletedAt(OffsetDateTime.now());
    when(wikiSpaceRepository.findById(1L)).thenReturn(Optional.of(deleted));

    assertThat(service.forWikiSpace(1L)).isEmpty();
    assertThat(service.forWikiPage(1L, 8L)).isEmpty();
  }

  private static WikiSpace space(Long id) {
    return WikiSpace.builder().id(id).name("Engineering").description(SECRET).build();
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

  @Test
  @DisplayName("a backlog preview summarises task counts and nothing a person wrote")
  void backlogShowsCountsOnly() {
    when(projectRepository.existsById(5L)).thenReturn(true);
    when(taskRepository.countByStatusForProject(5L)).thenReturn(List.<Object[]>of(
        new Object[] {TaskStatus.BACKLOG, 10L}, new Object[] {TaskStatus.IN_PROGRESS, 3L},
        new Object[] {TaskStatus.DONE, 6L}, new Object[] {TaskStatus.CANCELLED, 1L}));

    LinkPreview preview = service.forBacklog(5L).orElseThrow();

    assertThat(preview.title()).isEqualTo("Backlog");
    assertThat(preview.summary()).isEqualTo("Backlog: 13 open tasks · 3 in progress · 6 done");
    verify(taskRepository).countByStatusForProject(5L);
    verify(taskRepository, never()).findByProjectIdNotDeleted(anyLong());
  }

  @Test
  @DisplayName("an empty backlog says so, and an unknown project is absent")
  void backlogEmptyAndMissing() {
    when(projectRepository.existsById(5L)).thenReturn(true);
    when(taskRepository.countByStatusForProject(5L)).thenReturn(List.of());
    assertThat(service.forBacklog(5L).orElseThrow().summary()).isEqualTo("Backlog: no tasks yet");

    when(projectRepository.existsById(99L)).thenReturn(false);
    assertThat(service.forBacklog(99L)).isEmpty();
    assertThat(service.forBacklog(null)).isEmpty();
  }
}
