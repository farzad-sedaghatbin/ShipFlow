package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskCycleHistoryRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies {@link PitchCycleAssignmentService#applyCycleToPitch} propagates a pitch's cycle
 * change to every non-deleted linked task (and its project reference) and records one
 * {@link TaskCycleHistory} snapshot per affected task.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PitchCycleAssignmentServiceTest {

  @Autowired
  private PitchCycleAssignmentService pitchCycleAssignmentService;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private TaskCycleHistoryRepository taskCycleHistoryRepository;

  private Project project;
  private Cycle cycleA;
  private Cycle cycleB;
  private Pitch pitch;

  @BeforeEach
  void setUp() {
    taskCycleHistoryRepository.deleteAll();
    taskRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    projectRepository.deleteAll();

    project = projectRepository.save(Project.builder().name("Shape Up Project").projectKey("PCA1")
        .projectType(ProjectType.SHAPE_UP).isActive(true).build());

    cycleA = cycleRepository.save(Cycle.builder().name("Cycle A").project(project)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build());

    cycleB = cycleRepository.save(Cycle.builder().name("Cycle B").project(project)
        .startDate(LocalDate.now().plusWeeks(7)).endDate(LocalDate.now().plusWeeks(13))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(false).build());

    pitch = pitchRepository.save(Pitch.builder().title("Test Pitch").status(PitchStatus.SHAPED)
        .appetiteDays(14).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
  }

  private Task saveTask(String title) {
    return taskRepository.save(Task.builder().title(title).status(TaskStatus.TODO)
        .priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE).pitch(pitch)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
  }

  @Test
  void applyCycleToPitch_WithCycle_ShouldPropagateCycleAndProjectToTasks() {
    Task task1 = saveTask("Task 1");
    Task task2 = saveTask("Task 2");

    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleA);

    Pitch reloadedPitch = pitchRepository.findById(pitch.getId()).orElseThrow();
    assertThat(reloadedPitch.getCycle().getId()).isEqualTo(cycleA.getId());

    Task reloadedTask1 = taskRepository.findById(task1.getId()).orElseThrow();
    Task reloadedTask2 = taskRepository.findById(task2.getId()).orElseThrow();
    assertThat(reloadedTask1.getCycle().getId()).isEqualTo(cycleA.getId());
    assertThat(reloadedTask1.getProject().getId()).isEqualTo(project.getId());
    assertThat(reloadedTask2.getCycle().getId()).isEqualTo(cycleA.getId());
    assertThat(reloadedTask2.getProject().getId()).isEqualTo(project.getId());

    List<TaskCycleHistory> history1 = taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(task1.getId());
    assertThat(history1).hasSize(1);
    assertThat(history1.get(0).getCycle().getId()).isEqualTo(cycleA.getId());
    assertThat(history1.get(0).getSource()).isEqualTo(TaskCycleHistory.ChangeSource.PITCH_CYCLE_CHANGE);

    List<TaskCycleHistory> history2 = taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(task2.getId());
    assertThat(history2).hasSize(1);
  }

  @Test
  void applyCycleToPitch_ReBet_ShouldFollowTasksToNewCycle() {
    Task task = saveTask("Task 1");

    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleA);
    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleB);

    Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
    assertThat(reloaded.getCycle().getId()).isEqualTo(cycleB.getId());

    List<TaskCycleHistory> history = taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(task.getId());
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getCycle().getId()).isEqualTo(cycleA.getId());
    assertThat(history.get(1).getCycle().getId()).isEqualTo(cycleB.getId());
  }

  @Test
  void applyCycleToPitch_WithNullCycle_ShouldClearTaskCycleButPreserveProject() {
    Task task = saveTask("Task 1");
    // Give the task a project reference up front, as createTask would.
    task.setProject(project);
    task = taskRepository.save(task);

    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleA);
    pitchCycleAssignmentService.applyCycleToPitch(pitch, null);

    Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
    assertThat(reloaded.getCycle()).isNull();
    // Un-betting shouldn't strip the task's project reference — it hasn't moved projects.
    assertThat(reloaded.getProject()).isNotNull();
    assertThat(reloaded.getProject().getId()).isEqualTo(project.getId());

    List<TaskCycleHistory> history = taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(task.getId());
    assertThat(history).hasSize(2);
    assertThat(history.get(1).getCycle()).isNull();
  }

  @Test
  void applyCycleToPitch_ShouldNotPropagateToSoftDeletedTasks() {
    Task deletedTask = saveTask("Deleted Task");
    deletedTask.setDeletedAt(LocalDateTime.now());
    deletedTask = taskRepository.save(deletedTask);

    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleA);

    Task reloaded = taskRepository.findById(deletedTask.getId()).orElseThrow();
    assertThat(reloaded.getCycle()).isNull();
    assertThat(taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(deletedTask.getId())).isEmpty();
  }

  @Test
  void applyCycleToPitch_WithNoLinkedTasks_ShouldOnlyUpdatePitch() {
    pitchCycleAssignmentService.applyCycleToPitch(pitch, cycleA);

    Pitch reloaded = pitchRepository.findById(pitch.getId()).orElseThrow();
    assertThat(reloaded.getCycle().getId()).isEqualTo(cycleA.getId());
  }
}
