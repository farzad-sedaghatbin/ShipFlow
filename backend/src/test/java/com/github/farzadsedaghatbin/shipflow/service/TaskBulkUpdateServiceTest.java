package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.BulkTaskUpdateRequest;
import com.github.farzadsedaghatbin.shipflow.dto.BulkUpdateResult;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BulkAction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link TaskService#bulkUpdate(BulkTaskUpdateRequest)}.
 *
 * <p>Each action is tested in isolation using mock tasks that belong to the same project.
 * Cross-project validation and edge cases (empty IDs, already-deleted tasks) are tested separately.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskService.bulkUpdate Tests")
class TaskBulkUpdateServiceTest {

  @Mock TaskRepository taskRepository;
  @Mock CycleRepository cycleRepository;
  @Mock PersonRepository personRepository;
  @Mock UserRepository userRepository;
  @Mock PitchRepository pitchRepository;
  @Mock HillChartPointRepository hillChartPointRepository;
  @Mock TaskAttachmentRepository attachmentRepository;
  @Mock DashboardNotificationService notificationService;
  @Mock MessageService messageService;
  @Mock ApplicationEventPublisher eventPublisher;
  @Mock PermissionService permissionService;
  @Mock ProjectService projectService;
  @Mock TaskCycleHistoryService taskCycleHistoryService;

  @InjectMocks TaskService taskService;

  private Project project;
  private Cycle cycle;
  private Task task1;
  private Task task2;
  private User currentUser;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setId(1L);

    cycle = new Cycle();
    cycle.setId(10L);
    cycle.setProject(project);

    task1 = new Task();
    task1.setId(101L);
    task1.setTitle("Task 1");
    task1.setStatus(TaskStatus.BACKLOG);
    task1.setPriority(TaskPriority.MEDIUM);
    task1.setCycle(cycle);

    task2 = new Task();
    task2.setId(102L);
    task2.setTitle("Task 2");
    task2.setStatus(TaskStatus.TODO);
    task2.setPriority(TaskPriority.LOW);
    task2.setCycle(cycle);

    currentUser = new User();
    currentUser.setId(1L);
    currentUser.setUsername("testuser");

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
    when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    // Default: grant DELETE permission (individual tests can override)
    when(permissionService.hasPermission(ResourceType.BACKLOG, PermissionType.DELETE)).thenReturn(true);
  }

  private BulkTaskUpdateRequest request(BulkAction action, String value) {
    BulkTaskUpdateRequest req = new BulkTaskUpdateRequest();
    req.setTaskIds(List.of(101L, 102L));
    req.setAction(action);
    req.setValue(value);
    return req;
  }

  @Nested
  @DisplayName("CHANGE_STATUS action")
  class ChangeStatus {

    @Test
    @DisplayName("updates all tasks to new status")
    void updatesStatus() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_STATUS, "IN_PROGRESS"));

      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(result.getFailureCount()).isZero();
      assertThat(task1.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
      assertThat(task2.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("sets completedAt when status changes to DONE")
    void setsCompletedAt() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      taskService.bulkUpdate(request(BulkAction.CHANGE_STATUS, "DONE"));

      assertThat(task1.getCompletedAt()).isNotNull();
      assertThat(task2.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("invalid status value produces per-task error")
    void invalidStatusProducesError() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_STATUS, "NOT_A_STATUS"));

      assertThat(result.getFailureCount()).isEqualTo(2);
      assertThat(result.getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("setting BACKLOG on a subtask produces a per-task error, not a bulk failure")
    void backlogOnSubtaskProducesError() {
      Task parent = new Task();
      parent.setId(200L);
      task1.setParentTask(parent);
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_STATUS, "BACKLOG"));

      // task1 is a subtask and must be rejected; task2 has no parent so it succeeds
      assertThat(result.getSuccessCount()).isEqualTo(1);
      assertThat(result.getFailureCount()).isEqualTo(1);
      assertThat(task2.getStatus()).isEqualTo(TaskStatus.BACKLOG);
      // task1 was already BACKLOG from setUp; rejection happens before any mutation so it's a
      // no-op either way, but the important assertion is the error being reported for task1.
      assertThat(result.getErrors()).anyMatch(e -> e.contains("101"));
    }
  }

  @Nested
  @DisplayName("CHANGE_PRIORITY action")
  class ChangePriority {

    @Test
    @DisplayName("updates all tasks to new priority")
    void updatesPriority() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_PRIORITY, "HIGH"));

      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(task1.getPriority()).isEqualTo(TaskPriority.HIGH);
      assertThat(task2.getPriority()).isEqualTo(TaskPriority.HIGH);
    }
  }

  @Nested
  @DisplayName("ASSIGN action")
  class Assign {

    @Test
    @DisplayName("assigns all tasks to the given person")
    void assignsTasks() {
      Person person = new Person();
      person.setId(50L);
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));
      when(personRepository.findById(50L)).thenReturn(Optional.of(person));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.ASSIGN, "50"));

      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(task1.getAssignee()).isEqualTo(person);
      assertThat(task2.getAssignee()).isEqualTo(person);
    }

    @Test
    @DisplayName("unassigns tasks when value is blank")
    void unassignsTasks() {
      Person person = new Person();
      task1.setAssignee(person);
      task2.setAssignee(person);
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      taskService.bulkUpdate(request(BulkAction.ASSIGN, ""));

      assertThat(task1.getAssignee()).isNull();
      assertThat(task2.getAssignee()).isNull();
    }

    @Test
    @DisplayName("returns error when person not found")
    void personNotFoundProducesError() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));
      when(personRepository.findById(999L)).thenReturn(Optional.empty());

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.ASSIGN, "999"));

      assertThat(result.getFailureCount()).isEqualTo(2);
      assertThat(result.getErrors()).allMatch(e -> e.contains("Person not found"));
    }
  }

  @Nested
  @DisplayName("ADD_TAG action")
  class AddTag {

    @Test
    @DisplayName("appends tag to tasks with no existing tags")
    void appendsTagWhenEmpty() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      taskService.bulkUpdate(request(BulkAction.ADD_TAG, "urgent"));

      assertThat(task1.getTags()).isEqualTo("urgent");
      assertThat(task2.getTags()).isEqualTo("urgent");
    }

    @Test
    @DisplayName("appends tag to tasks that already have tags")
    void appendsTagToExisting() {
      task1.setTags("backend");
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      taskService.bulkUpdate(request(BulkAction.ADD_TAG, "frontend"));

      assertThat(task1.getTags()).isEqualTo("backend,frontend");
      assertThat(task2.getTags()).isEqualTo("frontend");
    }

    @Test
    @DisplayName("does not duplicate an existing tag")
    void doesNotDuplicateTag() {
      task1.setTags("backend");
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      taskService.bulkUpdate(request(BulkAction.ADD_TAG, "backend"));

      assertThat(task1.getTags()).isEqualTo("backend");
    }
  }

  @Nested
  @DisplayName("DELETE action")
  class Delete {

    @Test
    @DisplayName("soft-deletes all tasks when DELETE permission is granted")
    void softDeletesTasks() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.DELETE, null));

      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(task1.getDeletedAt()).isNotNull();
      assertThat(task2.getDeletedAt()).isNotNull();
      assertThat(task1.getDeletedBy()).isEqualTo(currentUser);
    }

    @Test
    @DisplayName("throws AccessDeniedException when caller lacks BACKLOG DELETE permission")
    void deniedWithoutDeletePermission() {
      when(permissionService.hasPermission(ResourceType.BACKLOG, PermissionType.DELETE)).thenReturn(false);

      BulkTaskUpdateRequest req = request(BulkAction.DELETE, null);

      assertThatThrownBy(() -> taskService.bulkUpdate(req))
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
  }

  @Nested
  @DisplayName("ADD_TAG — exact token deduplication")
  class AddTagDedup {

    @Test
    @DisplayName("does not add tag that is a substring of an existing tag")
    void substringIsNotADuplicate() {
      task1.setTags("backend");
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      // "end" is a substring of "backend" but NOT the same token — should be added
      taskService.bulkUpdate(request(BulkAction.ADD_TAG, "end"));

      assertThat(task1.getTags()).isEqualTo("backend,end");
    }

    @Test
    @DisplayName("rejects a tag value that contains a comma")
    void rejectsTagWithComma() {
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.ADD_TAG, "foo,bar"));

      assertThat(result.getFailureCount()).isEqualTo(2);
      assertThat(result.getErrors()).allMatch(e -> e.contains("commas"));
    }
  }

  @Nested
  @DisplayName("Skipped / missing IDs reported as failures")
  class SkippedIds {

    @Test
    @DisplayName("missing task ID is reported as a failure")
    void missingIdReportedAsFailure() {
      // Only task1 is returned — task2 (102) is missing
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_PRIORITY, "HIGH"));

      assertThat(result.getSuccessCount()).isEqualTo(1);
      assertThat(result.getFailureCount()).isEqualTo(1);
      assertThat(result.getErrors()).anyMatch(e -> e.contains("102") && e.contains("not found"));
    }

    @Test
    @DisplayName("already-deleted task ID is reported as a failure")
    void deletedIdReportedAsFailure() {
      task2.setDeletedAt(java.time.LocalDateTime.now());
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkUpdateResult result = taskService.bulkUpdate(request(BulkAction.CHANGE_PRIORITY, "HIGH"));

      assertThat(result.getSuccessCount()).isEqualTo(1);
      assertThat(result.getFailureCount()).isEqualTo(1);
      assertThat(result.getErrors()).anyMatch(e -> e.contains("102") && e.contains("deleted"));
    }
  }

  @Nested
  @DisplayName("Cross-project validation")
  class CrossProjectValidation {

    @Test
    @DisplayName("rejects request when tasks span multiple projects")
    void rejectsCrossProjectTasks() {
      Project project2 = new Project();
      project2.setId(2L);
      Cycle cycle2 = new Cycle();
      cycle2.setId(20L);
      cycle2.setProject(project2);
      Task taskInOtherProject = new Task();
      taskInOtherProject.setId(201L);
      taskInOtherProject.setCycle(cycle2);

      when(taskRepository.findAllById(List.of(101L, 201L))).thenReturn(List.of(task1, taskInOtherProject));

      BulkTaskUpdateRequest req = new BulkTaskUpdateRequest();
      req.setTaskIds(List.of(101L, 201L));
      req.setAction(BulkAction.CHANGE_STATUS);
      req.setValue("DONE");

      assertThatThrownBy(() -> taskService.bulkUpdate(req))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("same project");
    }

    @Test
    @DisplayName("reports deleted task IDs as failures rather than silently dropping them")
    void deletedTasksReportedAsFailures() {
      task1.setDeletedAt(java.time.LocalDateTime.now());
      task2.setDeletedAt(java.time.LocalDateTime.now());
      when(taskRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(task1, task2));

      BulkTaskUpdateRequest req = new BulkTaskUpdateRequest();
      req.setTaskIds(List.of(101L, 102L));
      req.setAction(BulkAction.CHANGE_STATUS);
      req.setValue("DONE");

      BulkUpdateResult result = taskService.bulkUpdate(req);

      assertThat(result.getSuccessCount()).isZero();
      assertThat(result.getFailureCount()).isEqualTo(2);
      assertThat(result.getErrors()).allMatch(e -> e.contains("deleted"));
    }
  }
}
