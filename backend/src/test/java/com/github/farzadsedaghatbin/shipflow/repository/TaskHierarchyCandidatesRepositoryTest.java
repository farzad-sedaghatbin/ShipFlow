package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/** The parent/subtask candidate pickers rely on these JPQL queries, so they run on a real database. */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository — subtask candidate queries")
class TaskHierarchyCandidatesRepositoryTest {

  @Autowired private TaskRepository taskRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private CycleRepository cycleRepository;

  private Project project;
  private Project otherProject;
  private Task self;

  @BeforeEach
  void setUp() {
    project = project("MAIN");
    otherProject = project("OTHR");
    Cycle cycle = cycleRepository.save(Cycle.builder().name("C").project(project).startDate(java.time.LocalDate.now())
        .endDate(java.time.LocalDate.now().plusDays(14)).isActive(true)
        .phase(com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase.SHAPING_BUILDING).build());
    self = save("Convert me", cycle, project, null);
  }

  private Project project(String key) {
    Project p = new Project();
    p.setName(key);
    p.setProjectKey(key);
    p.setProjectType(ProjectType.SCRUM);
    p.setIsActive(true);
    return projectRepository.save(p);
  }

  private Task save(String title, Cycle cycle, Project p, Task parent) {
    return taskRepository.saveAndFlush(Task.builder().title(title).cycle(cycle).project(p).parentTask(parent)
        .status(TaskStatus.TODO).priority(TaskPriority.MEDIUM).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());
  }

  private List<Task> parents(String q, Long qId) {
    return taskRepository.findParentCandidates(project.getId(), self.getId(), q, qId, PageRequest.of(0, 20));
  }

  private List<Task> children(String q, Long qId) {
    return taskRepository.findSubtaskCandidates(project.getId(), self.getId(), q, qId, PageRequest.of(0, 20));
  }

  @Test
  @DisplayName("parent candidates: same project, top-level, live, not the task itself")
  void parentCandidates() {
    Task epicLike = save("Checkout flow", null, project, null);
    Task sub = save("Checkout sub", null, project, epicLike);
    save("Foreign", null, otherProject, null);
    Task gone = save("Deleted checkout", null, project, null);
    gone.setDeletedAt(LocalDateTime.now());
    taskRepository.saveAndFlush(gone);

    List<Task> result = parents("checkout", -1L);

    assertThat(result).extracting(Task::getId).containsExactly(epicLike.getId());
    assertThat(result).doesNotContain(sub, self, gone);
  }

  @Test
  @DisplayName("matches by numeric id as well as title")
  void matchesById() {
    Task other = save("Unrelated title", null, project, null);
    assertThat(parents("nomatch", other.getId())).containsExactly(other);
  }

  @Test
  @DisplayName("subtask candidates exclude tasks that already have subtasks")
  void childCandidatesExcludeParents() {
    Task leaf = save("Leaf", null, project, null);
    Task withKid = save("Has kid", null, project, null);
    save("kid", null, project, withKid);

    assertThat(children("", -1L)).contains(leaf).doesNotContain(withKid, self);
    assertThat(parents("", -1L)).contains(withKid, leaf);
  }
}
