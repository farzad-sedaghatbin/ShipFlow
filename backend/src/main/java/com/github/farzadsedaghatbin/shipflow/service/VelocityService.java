package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.VelocityPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes velocity chart data across all sprints (cycles) for a project. Velocity is defined as
 * the story points completed (DONE tasks) per sprint, alongside the planned story points (all tasks
 * with story points in the sprint).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VelocityService {

  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;

  /**
   * Compute velocity for each cycle of the project, ordered by start date ascending.
   *
   * <p>Uses a single batch query for all project tasks to avoid N+1 queries (one per cycle).
   *
   * @param projectId the ID of the project
   * @return list of {@link VelocityPointDTO} one per cycle
   */
  public List<VelocityPointDTO> computeVelocity(Long projectId) {
    // Query ascending so the velocity chart naturally shows oldest sprint first
    List<Cycle> orderedCycles = cycleRepository.findByProjectIdOrderByStartDateAsc(projectId);

    // Single batch query for all non-deleted tasks in the project — avoids N+1 (one query per
    // cycle)
    List<Task> allTasks = taskRepository.findByProjectIdNotDeleted(projectId);
    Map<Long, List<Task>> byCycle =
        allTasks.stream()
            .filter(t -> t.getCycle() != null)
            .collect(Collectors.groupingBy(t -> t.getCycle().getId()));

    return orderedCycles.stream()
        .map(
            cycle -> {
              List<Task> tasks = byCycle.getOrDefault(cycle.getId(), List.of());

              int planned =
                  tasks.stream()
                      .filter(t -> t.getStoryPoints() != null)
                      .mapToInt(Task::getStoryPoints)
                      .sum();

              int completed =
                  tasks.stream()
                      .filter(t -> t.getStoryPoints() != null && t.getStatus() == TaskStatus.DONE)
                      .mapToInt(Task::getStoryPoints)
                      .sum();

              return VelocityPointDTO.builder()
                  .cycleId(cycle.getId())
                  .cycleName(cycle.getName())
                  .plannedPoints(planned)
                  .completedPoints(completed)
                  .build();
            })
        .toList();
  }
}
