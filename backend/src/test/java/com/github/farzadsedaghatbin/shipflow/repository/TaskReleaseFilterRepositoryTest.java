package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Backlog's release filter used to run in the browser over one page of 10 tasks, so it showed
 * only the matches that happened to be on that page. These queries now filter by release
 * themselves; this proves it on a real database, with the matches spread beyond the first page.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository — release filter")
class TaskReleaseFilterRepositoryTest {

  /** First page of 10, newest first — what the Backlog list asks for. */
  private static final Pageable FIRST_PAGE = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

  @Autowired private TaskRepository taskRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private CycleRepository cycleRepository;
  @Autowired private ReleaseRepository releaseRepository;
  @Autowired private PersonRepository personRepository;

  private Project project;
  private Cycle cycleA;
  private Cycle cycleB;
  private Release release;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setName("MAIN");
    project.setProjectKey("MAIN");
    project.setProjectType(ProjectType.SHAPE_UP);
    project.setIsActive(true);
    project = projectRepository.save(project);
    cycleA = cycle("Cycle A");
    cycleB = cycle("Cycle B");
    release = releaseRepository.save(Release.builder().name("Release 1.7.4").version("1.7.4")
        .status(ReleaseStatus.PLANNING).project(project).build());
    Release other = releaseRepository.save(Release.builder().name("Release 1.8.0").version("1.8.0")
        .status(ReleaseStatus.PLANNING).project(project).build());

    LocalDateTime base = LocalDateTime.now().minusDays(60);
    // The oldest tasks carry the release, so none of them is in the newest 10.
    save("A in release 1", cycleA, release, base.plusMinutes(1));
    save("A in release 2", cycleA, release, base.plusMinutes(2));
    save("B in release", cycleB, release, base.plusMinutes(3));
    save("B other release", cycleB, other, base.plusMinutes(4));
    for (int i = 0; i < 12; i++) {
      save("A no release " + i, cycleA, null, base.plusDays(1).plusMinutes(i));
      save("B no release " + i, cycleB, null, base.plusDays(2).plusMinutes(i));
    }
    Task deleted = save("A in release, deleted", cycleA, release, base.plusMinutes(5));
    deleted.setDeletedAt(LocalDateTime.now());
    taskRepository.saveAndFlush(deleted);
  }

  private Cycle cycle(String name) {
    return cycleRepository.save(Cycle.builder().name(name).project(project).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(14)).isActive(true).phase(CyclePhase.SHAPING_BUILDING).build());
  }

  private Task save(String title, Cycle cycle, Release targetRelease, LocalDateTime createdAt) {
    Task task = taskRepository.saveAndFlush(Task.builder().title(title).cycle(cycle).project(project)
        .targetRelease(targetRelease).status(TaskStatus.TODO).priority(TaskPriority.MEDIUM).createdAt(createdAt)
        .updatedAt(createdAt).build());
    // createdAt may be stamped on insert; set it again so the ordering in this test is deterministic.
    task.setCreatedAt(createdAt);
    return taskRepository.saveAndFlush(task);
  }

  @Test
  @DisplayName("all cycles: every task of the release is returned, not just those among the newest 10")
  void allCyclesFindsEveryTaskOfTheRelease() {
    Page<Task> page = taskRepository.findAllWithFilters(null, null, null, null, null, release.getId(), FIRST_PAGE);

    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getContent()).extracting(Task::getTitle)
        .containsExactlyInAnyOrder("A in release 1", "A in release 2", "B in release");
  }

  @Test
  @DisplayName("one cycle: only that cycle's tasks of the release")
  void cycleScopedFilter() {
    Page<Task> page = taskRepository.findByCycleIdWithFilters(cycleA.getId(), null, null, null, null,
        null, release.getId(), FIRST_PAGE);

    assertThat(page.getContent()).extracting(Task::getTitle)
        .containsExactlyInAnyOrder("A in release 1", "A in release 2");
  }

  @Test
  @DisplayName("project scope: every live task of the release")
  void projectScopedFilter() {
    Page<Task> page = taskRepository.findByProjectIdWithFilters(project.getId(), null, null, null, null,
        null, release.getId(), FIRST_PAGE);

    assertThat(page.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("no release chosen: tasks without a release are still returned")
  void noReleaseFilterKeepsTasksWithoutARelease() {
    Page<Task> all = taskRepository.findAllWithFilters(null, null, null, null, null, null, FIRST_PAGE);
    Page<Task> inCycle = taskRepository.findByCycleIdWithFilters(cycleA.getId(), null, null, null, null, null,
        null, FIRST_PAGE);

    // 4 with a release + 24 without; the soft-deleted one is left out.
    assertThat(all.getTotalElements()).isEqualTo(28);
    assertThat(inCycle.getTotalElements()).isEqualTo(14);
  }

  @Test
  @DisplayName("adding the creator filter narrows the release's tasks, it never finds more of them")
  void creatorFilterNarrowsTheRelease() {
    Person creator = personRepository.save(Person.builder().name("Majid").isActive(true)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    // One of the release's three tasks is the creator's; so are plenty of newer tasks outside it.
    Task ofRelease = taskRepository.findAllWithFilters(null, null, null, null, null, release.getId(), FIRST_PAGE)
        .getContent().get(0);
    ofRelease.setCreatedBy(creator);
    taskRepository.saveAndFlush(ofRelease);
    taskRepository.findByCycleIdWithFilters(cycleB.getId(), null, null, null, null, null, null, FIRST_PAGE)
        .forEach(t -> {
          if (t.getTargetRelease() == null) {
            t.setCreatedBy(creator);
            taskRepository.saveAndFlush(t);
          }
        });

    long releaseOnly = taskRepository
        .findAllWithFilters(null, null, null, null, null, release.getId(), FIRST_PAGE).getTotalElements();
    Page<Task> releaseAndCreator = taskRepository.findAllWithFilters(null, null, null, null,
        List.of(creator.getId()), release.getId(), FIRST_PAGE);

    assertThat(releaseOnly).isEqualTo(3);
    assertThat(releaseAndCreator.getTotalElements()).isEqualTo(1);
    assertThat(releaseAndCreator.getContent()).containsExactly(ofRelease);
  }
}
