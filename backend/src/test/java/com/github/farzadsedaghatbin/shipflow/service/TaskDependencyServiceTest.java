package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

  @Mock
  private TaskDependencyRepository taskDependencyRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private LocalizationService localizationService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private TaskDependencyService taskDependencyService;

  private Cycle testCycle;
  private Task sourceTask;
  private Task targetTask;
  private Task anotherTask;
  private TaskDependency testDependency;

  @BeforeEach
  void setUp() {
    lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("task.not.found"))
        return "Task not found";
      return key;
    });
    lenient().when(messageService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("task.not.found"))
        return "Task not found";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("task.different.cycle"))
        return "Tasks must belong to the same cycle";
      if (key.contains("task.self.dependency"))
        return "A task cannot depend on itself";
      if (key.contains("dependency.circular"))
        return "Circular dependency detected - would create circular reference";
      if (key.contains("dependency.self.reference"))
        return "Task cannot depend on itself";
      if (key.contains("dependency.same.cycle.required"))
        return "Tasks must be in the same cycle";
      if (key.contains("dependency.already.exists"))
        return "Dependency already exists";
      if (key.contains("dependency.not.found"))
        return "Dependency not found between tasks";
      if (key.contains("task.not.found"))
        return "Task not found";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("task.different.cycle"))
        return "Tasks must belong to the same cycle";
      if (key.contains("task.self.dependency"))
        return "A task cannot depend on itself";
      if (key.contains("dependency.circular"))
        return "Circular dependency detected - would create circular reference";
      if (key.contains("dependency.self.reference"))
        return "Task cannot depend on itself";
      if (key.contains("dependency.same.cycle.required"))
        return "Tasks must be in the same cycle";
      if (key.contains("dependency.already.exists"))
        return "Dependency already exists";
      if (key.contains("dependency.not.found"))
        return "Dependency not found between tasks";
      if (key.contains("task.not.found"))
        return "Task not found";
      return key;
    });

    testCycle = Cycle.builder().id(1L).name("Test Cycle").build();

    sourceTask = Task.builder().id(1L).title("Source Task").status(TaskStatus.TODO).priority(TaskPriority.MEDIUM)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    targetTask = Task.builder().id(2L).title("Target Task").status(TaskStatus.TODO).priority(TaskPriority.MEDIUM)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    anotherTask = Task.builder().id(3L).title("Another Task").status(TaskStatus.TODO).priority(TaskPriority.MEDIUM)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    testDependency = TaskDependency.builder().id(1L).sourceTask(sourceTask).targetTask(targetTask)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();
  }

  @Test
  void addDependency_Success() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskDependencyRepository.existsBetweenTasks(1L, 2L)).thenReturn(false);
    when(taskDependencyRepository.save(any(TaskDependency.class))).thenReturn(testDependency);

    // When
    TaskDependencyDTO result = taskDependencyService.addDependency(1L, request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSourceTaskId()).isEqualTo(1L);
    assertThat(result.getTargetTaskId()).isEqualTo(2L);
    assertThat(result.getDependencyType()).isEqualTo(DependencyType.BLOCKS);
    verify(taskDependencyRepository).save(any(TaskDependency.class));
  }

  @Test
  void addDependency_SourceTaskNotFound_ThrowsException() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Source task not found");
  }

  @Test
  void addDependency_TargetTaskNotFound_ThrowsException() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(999L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Target task not found");
  }

  @Test
  void addDependency_TasksInDifferentCycles_ThrowsException() {
    // Given
    Cycle differentCycle = Cycle.builder().id(2L).name("Different Cycle").build();
    targetTask.setCycle(differentCycle);

    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("same cycle");
  }

  @Test
  void addDependency_SelfDependency_ThrowsException() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(1L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("cannot depend on itself");
  }

  @Test
  void addDependency_DuplicateDependency_ThrowsException() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskDependencyRepository.existsBetweenTasks(1L, 2L)).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("already exists");
  }

  @Test
  void addDependency_CircularDependency_ThrowsException() {
    // Given - Create a scenario where target already depends on source
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.BLOCKS).build();

    TaskDependency existingDependency = TaskDependency.builder().id(2L).sourceTask(targetTask)
        .targetTask(sourceTask).dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(2L, 2L))
        .thenReturn(Arrays.asList(existingDependency));

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("circular reference");
  }

  @Test
  void addDependency_DefaultDependencyType() {
    // Given
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L).build(); // No
    // dependency
    // type
    // specified

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskDependencyRepository.existsBetweenTasks(1L, 2L)).thenReturn(false);
    when(taskDependencyRepository.save(any(TaskDependency.class))).thenReturn(testDependency);

    // When
    TaskDependencyDTO result = taskDependencyService.addDependency(1L, request);

    // Then
    assertThat(result.getDependencyType()).isEqualTo(DependencyType.BLOCKS);
  }

  @Test
  void removeDependency_Success() {
    // Given
    when(taskDependencyRepository.existsById(1L)).thenReturn(true);

    // When
    taskDependencyService.removeDependency(1L);

    // Then
    verify(taskDependencyRepository).deleteById(1L);
  }

  @Test
  void removeDependency_NotFound_ThrowsException() {
    // Given
    when(taskDependencyRepository.existsById(999L)).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.removeDependency(999L))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not found");
  }

  @Test
  void getDependenciesForTask_Success() {
    // Given
    TaskDependency outgoingDep = TaskDependency.builder().id(1L).sourceTask(sourceTask).targetTask(targetTask)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();

    TaskDependency incomingDep = TaskDependency.builder().id(2L).sourceTask(anotherTask).targetTask(sourceTask)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();

    when(taskRepository.existsById(1L)).thenReturn(true);
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L))
        .thenReturn(Arrays.asList(outgoingDep, incomingDep));

    // When
    Map<String, List<TaskDependencyDTO>> result = taskDependencyService.getDependenciesForTask(1L);

    // Then
    assertThat(result).containsKeys("blocking", "blockedBy", "related");
    assertThat(result.get("related")).isEmpty();
    assertThat(result.get("blocking")).hasSize(1);
    assertThat(result.get("blockedBy")).hasSize(1);
    assertThat(result.get("blocking").get(0).getOtherTaskId()).isEqualTo(2L);
    assertThat(result.get("blockedBy").get(0).getOtherTaskId()).isEqualTo(3L);
  }

  @Test
  void getDependenciesForTask_TaskNotFound_ThrowsException() {
    // Given
    when(taskRepository.existsById(999L)).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.getDependenciesForTask(999L))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not found");
  }

  @Test
  void getBlockingDependencies_Success() {
    // Given
    when(taskRepository.existsById(1L)).thenReturn(true);
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L))
        .thenReturn(Arrays.asList(testDependency));

    // When
    List<TaskDependencyDTO> result = taskDependencyService.getBlockingDependencies(1L);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOtherTaskId()).isEqualTo(2L);
  }

  @Test
  void getBlockedByDependencies_Success() {
    // Given
    TaskDependency blockingDep = TaskDependency.builder().id(2L).sourceTask(targetTask).targetTask(sourceTask)
        .dependencyType(DependencyType.BLOCKS).createdAt(LocalDateTime.now()).build();

    when(taskRepository.existsById(1L)).thenReturn(true);
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L))
        .thenReturn(Arrays.asList(blockingDep));

    // When
    List<TaskDependencyDTO> result = taskDependencyService.getBlockedByDependencies(1L);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOtherTaskId()).isEqualTo(2L);
  }

  @Test
  void addDependency_ComplexCircularDependency_ThrowsException() {
    // Given - Task1 -> Task2 -> Task3, trying to add Task3 -> Task1
    Task task3 = Task.builder().id(3L).title("Task 3").cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();

    TaskDependency dep1to2 = TaskDependency.builder().sourceTask(sourceTask).targetTask(targetTask).build();

    TaskDependency dep2to3 = TaskDependency.builder().sourceTask(targetTask).targetTask(task3).build();

    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(1L)
        .dependencyType(DependencyType.BLOCKS).build();

    when(taskRepository.findById(3L)).thenReturn(Optional.of(task3));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L)).thenReturn(Arrays.asList(dep1to2));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(2L, 2L))
        .thenReturn(Arrays.asList(dep1to2, dep2to3));

    // When & Then
    assertThatThrownBy(() -> taskDependencyService.addDependency(3L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("circular reference");
  }

  private TaskDependency dep(long id, Task source, Task target, DependencyType type) {
    return TaskDependency.builder().id(id).sourceTask(source).targetTask(target).dependencyType(type)
        .createdAt(LocalDateTime.now()).build();
  }

  @Test
  void getDependenciesForTask_RelatedToIsNeverABlocker_OnEitherSide() {
    TaskDependency related = dep(1L, sourceTask, targetTask, DependencyType.RELATED_TO);

    when(taskRepository.existsById(1L)).thenReturn(true);
    when(taskRepository.existsById(2L)).thenReturn(true);
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L)).thenReturn(List.of(related));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(2L, 2L)).thenReturn(List.of(related));

    Map<String, List<TaskDependencyDTO>> fromSource = taskDependencyService.getDependenciesForTask(1L);
    Map<String, List<TaskDependencyDTO>> fromTarget = taskDependencyService.getDependenciesForTask(2L);

    assertThat(fromSource.get("blocking")).isEmpty();
    assertThat(fromSource.get("blockedBy")).isEmpty();
    assertThat(fromSource.get("related")).extracting(TaskDependencyDTO::getOtherTaskId).containsExactly(2L);

    // The bug: the target used to see the source under "blocked by"
    assertThat(fromTarget.get("blocking")).isEmpty();
    assertThat(fromTarget.get("blockedBy")).isEmpty();
    assertThat(fromTarget.get("related")).extracting(TaskDependencyDTO::getOtherTaskId).containsExactly(1L);
  }

  @Test
  void getDependenciesForTask_DependsOnMeansTheTargetIsTheBlocker() {
    // source DEPENDS_ON target  =>  target blocks source
    TaskDependency dependsOn = dep(1L, sourceTask, targetTask, DependencyType.DEPENDS_ON);

    when(taskRepository.existsById(1L)).thenReturn(true);
    when(taskRepository.existsById(2L)).thenReturn(true);
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L)).thenReturn(List.of(dependsOn));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(2L, 2L)).thenReturn(List.of(dependsOn));

    Map<String, List<TaskDependencyDTO>> waiting = taskDependencyService.getDependenciesForTask(1L);
    Map<String, List<TaskDependencyDTO>> blocker = taskDependencyService.getDependenciesForTask(2L);

    assertThat(waiting.get("blockedBy")).extracting(TaskDependencyDTO::getOtherTaskId).containsExactly(2L);
    assertThat(waiting.get("blocking")).isEmpty();
    assertThat(blocker.get("blocking")).extracting(TaskDependencyDTO::getOtherTaskId).containsExactly(1L);
    assertThat(blocker.get("blockedBy")).isEmpty();
  }

  @Test
  void addDependency_RelatedToBothWays_IsRejectedAsDuplicateNotCircular() {
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(1L)
        .dependencyType(DependencyType.RELATED_TO).build();

    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskDependencyRepository.existsBetweenTasks(2L, 1L)).thenReturn(true);

    assertThatThrownBy(() -> taskDependencyService.addDependency(2L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("already exists");
  }

  @Test
  void addDependency_RelatedTo_NeverCountsAsACycle() {
    // 2 BLOCKS 1 exists; relating 1 to 2 must not be treated as a loop (it is a duplicate pair in
    // reality, but the cycle check itself must ignore RELATED_TO)
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(3L)
        .dependencyType(DependencyType.RELATED_TO).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(3L)).thenReturn(Optional.of(anotherTask));
    when(taskDependencyRepository.existsBetweenTasks(1L, 3L)).thenReturn(false);
    when(taskDependencyRepository.save(any(TaskDependency.class)))
        .thenReturn(dep(9L, sourceTask, anotherTask, DependencyType.RELATED_TO));

    TaskDependencyDTO result = taskDependencyService.addDependency(1L, request);

    assertThat(result.getDependencyType()).isEqualTo(DependencyType.RELATED_TO);
    verify(taskDependencyRepository, never()).findBySourceTaskIdOrTargetTaskId(anyLong(), anyLong());
  }

  @Test
  void addDependency_DependsOnThatClosesALoop_IsRejected() {
    // 1 BLOCKS 2, 2 BLOCKS 3, then "1 DEPENDS_ON 3" (3 must finish before 1) is a real loop
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(3L)
        .dependencyType(DependencyType.DEPENDS_ON).build();
    TaskDependency oneBlocksTwo = dep(1L, sourceTask, targetTask, DependencyType.BLOCKS);
    TaskDependency twoBlocksThree = dep(2L, targetTask, anotherTask, DependencyType.BLOCKS);

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(3L)).thenReturn(Optional.of(anotherTask));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(1L, 1L)).thenReturn(List.of(oneBlocksTwo));
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(2L, 2L))
        .thenReturn(List.of(oneBlocksTwo, twoBlocksThree));

    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("circular reference");
  }

  @Test
  void addDependency_DependsOnThatIsNotALoop_IsAllowed() {
    // 1 BLOCKS 2, 2 BLOCKS 3, then "3 DEPENDS_ON 1" just says 1 blocks 3 as well: no loop
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(1L)
        .dependencyType(DependencyType.DEPENDS_ON).build();
    TaskDependency oneBlocksTwo = dep(1L, sourceTask, targetTask, DependencyType.BLOCKS);
    TaskDependency twoBlocksThree = dep(2L, targetTask, anotherTask, DependencyType.BLOCKS);

    when(taskRepository.findById(3L)).thenReturn(Optional.of(anotherTask));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskDependencyRepository.existsBetweenTasks(3L, 1L)).thenReturn(false);
    // blocked = 3 (the dependent); nothing is blocked by 3
    when(taskDependencyRepository.findBySourceTaskIdOrTargetTaskId(3L, 3L)).thenReturn(List.of(twoBlocksThree));
    when(taskDependencyRepository.save(any(TaskDependency.class)))
        .thenReturn(dep(9L, anotherTask, sourceTask, DependencyType.DEPENDS_ON));

    TaskDependencyDTO result = taskDependencyService.addDependency(3L, request);

    assertThat(result.getDependencyType()).isEqualTo(DependencyType.DEPENDS_ON);
  }

  @Test
  void addDependency_TasksWithoutCycle_ShareAProject_IsAllowed() {
    com.github.farzadsedaghatbin.shipflow.entity.Project project = com.github.farzadsedaghatbin.shipflow.entity.Project
        .builder().id(7L).build();
    sourceTask.setCycle(null);
    targetTask.setCycle(null);
    sourceTask.setProject(project);
    targetTask.setProject(project);
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L)
        .dependencyType(DependencyType.RELATED_TO).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));
    when(taskDependencyRepository.existsBetweenTasks(1L, 2L)).thenReturn(false);
    when(taskDependencyRepository.save(any(TaskDependency.class)))
        .thenReturn(dep(9L, sourceTask, targetTask, DependencyType.RELATED_TO));

    assertThat(taskDependencyService.addDependency(1L, request)).isNotNull();
  }

  @Test
  void addDependency_TaskWithoutCycleAndProject_IsRejectedNotNpe() {
    sourceTask.setCycle(null);
    CreateTaskDependencyRequest request = CreateTaskDependencyRequest.builder().targetTaskId(2L).build();

    when(taskRepository.findById(1L)).thenReturn(Optional.of(sourceTask));
    when(taskRepository.findById(2L)).thenReturn(Optional.of(targetTask));

    assertThatThrownBy(() -> taskDependencyService.addDependency(1L, request))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("same cycle");
  }
}
