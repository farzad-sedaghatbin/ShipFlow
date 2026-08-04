package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.TaskCycleHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TaskCycleHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCycleHistoryServiceTest {

  @Mock
  private TaskCycleHistoryRepository taskCycleHistoryRepository;

  @InjectMocks
  private TaskCycleHistoryService taskCycleHistoryService;

  private Task task;
  private Cycle cycle;

  @BeforeEach
  void setUp() {
    cycle = Cycle.builder().id(1L).name("Cycle A").build();
    task = Task.builder().id(1L).title("Task 1").status(TaskStatus.TODO).priority(TaskPriority.MEDIUM)
        .category(TaskCategory.PITCH_SCOPE).cycle(cycle).build();
  }

  @Test
  void recordSnapshot_ShouldBuildRowFromTaskCurrentState() {
    when(taskCycleHistoryRepository.save(any(TaskCycleHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TaskCycleHistory result = taskCycleHistoryService.recordSnapshot(task, TaskCycleHistory.ChangeSource.TASK_CREATED);

    assertThat(result.getTask()).isEqualTo(task);
    assertThat(result.getCycle()).isEqualTo(cycle);
    assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(result.getSource()).isEqualTo(TaskCycleHistory.ChangeSource.TASK_CREATED);
  }

  @Test
  void recordSnapshot_WithNoCycle_ShouldStoreNullCycle() {
    task.setCycle(null);
    when(taskCycleHistoryRepository.save(any(TaskCycleHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TaskCycleHistory result = taskCycleHistoryService.recordSnapshot(task, TaskCycleHistory.ChangeSource.TASK_CREATED);

    assertThat(result.getCycle()).isNull();
  }

  @Test
  void recordSnapshots_ShouldBatchSaveOneRowPerTask() {
    Task task2 = Task.builder().id(2L).title("Task 2").status(TaskStatus.DONE).priority(TaskPriority.HIGH)
        .category(TaskCategory.PITCH_SCOPE).cycle(cycle).build();

    when(taskCycleHistoryRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<TaskCycleHistory> result = taskCycleHistoryService.recordSnapshots(
        List.of(task, task2), TaskCycleHistory.ChangeSource.PITCH_CYCLE_CHANGE);

    assertThat(result).hasSize(2);
    assertThat(result).allSatisfy(h -> assertThat(h.getSource())
        .isEqualTo(TaskCycleHistory.ChangeSource.PITCH_CYCLE_CHANGE));

    ArgumentCaptor<List<TaskCycleHistory>> captor = ArgumentCaptor.forClass(List.class);
    verify(taskCycleHistoryRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
  }

  @Test
  void getHistoryForTask_ShouldMapEntitiesToDTOsInOrder() {
    TaskCycleHistory h1 = TaskCycleHistory.builder().id(10L).task(task).cycle(cycle).status(TaskStatus.TODO)
        .source(TaskCycleHistory.ChangeSource.TASK_CREATED).recordedAt(LocalDateTime.now().minusDays(1)).build();
    TaskCycleHistory h2 = TaskCycleHistory.builder().id(11L).task(task).cycle(null).status(TaskStatus.DONE)
        .source(TaskCycleHistory.ChangeSource.STATUS_CHANGE).recordedAt(LocalDateTime.now()).build();

    when(taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(1L)).thenReturn(List.of(h1, h2));

    List<TaskCycleHistoryDTO> result = taskCycleHistoryService.getHistoryForTask(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCycleId()).isEqualTo(1L);
    assertThat(result.get(0).getCycleName()).isEqualTo("Cycle A");
    assertThat(result.get(0).getSource()).isEqualTo(TaskCycleHistory.ChangeSource.TASK_CREATED);
    assertThat(result.get(1).getCycleId()).isNull();
    assertThat(result.get(1).getStatus()).isEqualTo(TaskStatus.DONE);
  }
}
