package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.SprintReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Computes a per-sprint summary report: planned vs completed story points, completion rate,
 * task counts by status, and a scope-change proxy (tasks added to the sprint after it started).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SprintReportService {

  private final TaskRepository taskRepository;

  /**
   * Compute the sprint report for the given cycle. The caller (SprintReportController) is
   * responsible for loading the cycle and enforcing project-scope authorization before calling
   * this method. Accepting the pre-loaded {@link Cycle} avoids a redundant DB query.
   *
   * @param cycle the sprint/cycle entity (must not be null)
   * @return the {@link SprintReportDTO} summary for the sprint
   */
  public SprintReportDTO computeSprintReport(Cycle cycle) {
    Long cycleId = cycle.getId();

    if (cycle.getStartDate() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cycle " + cycleId + " has no start date");
    }

    List<Task> tasks = taskRepository.findByCycleIdNotDeleted(cycleId);

    log.info("SprintReportService: cycleId={} tasks={}", cycleId, tasks.size());

    int plannedPoints =
        tasks.stream()
            .filter(t -> t.getStoryPoints() != null)
            .mapToInt(Task::getStoryPoints)
            .sum();

    int completedPoints =
        tasks.stream()
            .filter(t -> t.getStoryPoints() != null && t.getStatus() == TaskStatus.DONE)
            .mapToInt(Task::getStoryPoints)
            .sum();

    double completionRate = plannedPoints == 0 ? 0.0 : completedPoints / (double) plannedPoints;

    // Pre-populate every TaskStatus at 0 so the response always carries all 7 keys.
    Map<String, Integer> taskCountByStatus = new LinkedHashMap<>();
    for (TaskStatus status : TaskStatus.values()) {
      taskCountByStatus.put(status.name(), 0);
    }
    for (Task task : tasks) {
      taskCountByStatus.merge(task.getStatus().name(), 1, Integer::sum);
    }

    List<Task> scopeAddedTasks =
        tasks.stream()
            .filter(t -> t.getCreatedAt() != null)
            .filter(t -> t.getCreatedAt().toLocalDate().isAfter(cycle.getStartDate()))
            .toList();

    int scopeAddedTaskCount = scopeAddedTasks.size();
    int scopeAddedPoints =
        scopeAddedTasks.stream()
            .filter(t -> t.getStoryPoints() != null)
            .mapToInt(Task::getStoryPoints)
            .sum();

    return SprintReportDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycle.getName())
        .startDate(cycle.getStartDate())
        .endDate(cycle.getEndDate())
        .plannedPoints(plannedPoints)
        .completedPoints(completedPoints)
        .completionRate(completionRate)
        .taskCountByStatus(taskCountByStatus)
        .scopeAddedTaskCount(scopeAddedTaskCount)
        .scopeAddedPoints(scopeAddedPoints)
        .build();
  }
}
