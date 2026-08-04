package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.TaskCycleHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import com.github.farzadsedaghatbin.shipflow.repository.TaskCycleHistoryRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and retrieves the {@link TaskCycleHistory} audit trail — "which cycle was this task
 * in, in which cycle did it complete". Snapshots are written via direct synchronous calls from
 * the primary service methods that change a task's cycle or status (never via an event
 * listener) so the audit trail is always consistent with the row it describes in the same
 * transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskCycleHistoryService {

  private final TaskCycleHistoryRepository taskCycleHistoryRepository;

  /** Build and save one snapshot row from the task's current cycle/status. */
  public TaskCycleHistory recordSnapshot(Task task, TaskCycleHistory.ChangeSource source) {
    TaskCycleHistory snapshot = TaskCycleHistory.builder()
        .task(task)
        .cycle(task.getCycle())
        .status(task.getStatus())
        .source(source)
        .build();
    return taskCycleHistoryRepository.save(snapshot);
  }

  /**
   * Build and save one snapshot row per task in a single batch insert. Used when a pitch's
   * cycle change propagates to many tasks at once ({@code PitchCycleAssignmentService}) so the
   * audit trail write doesn't degrade into N individual inserts.
   */
  public List<TaskCycleHistory> recordSnapshots(List<Task> tasks, TaskCycleHistory.ChangeSource source) {
    List<TaskCycleHistory> snapshots = tasks.stream()
        .map(task -> TaskCycleHistory.builder()
            .task(task)
            .cycle(task.getCycle())
            .status(task.getStatus())
            .source(source)
            .build())
        .collect(Collectors.toList());
    return taskCycleHistoryRepository.saveAll(snapshots);
  }

  @Transactional(readOnly = true)
  public List<TaskCycleHistoryDTO> getHistoryForTask(Long taskId) {
    return taskCycleHistoryRepository.findByTaskIdOrderByRecordedAtAsc(taskId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  private TaskCycleHistoryDTO toDTO(TaskCycleHistory h) {
    return TaskCycleHistoryDTO.builder()
        .id(h.getId())
        .cycleId(h.getCycle() != null ? h.getCycle().getId() : null)
        .cycleName(h.getCycle() != null ? h.getCycle().getName() : null)
        .status(h.getStatus())
        .source(h.getSource())
        .recordedAt(h.getRecordedAt())
        .build();
  }
}
