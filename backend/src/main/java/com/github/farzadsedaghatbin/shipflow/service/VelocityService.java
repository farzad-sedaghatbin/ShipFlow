package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.VelocityPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
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
   * @param projectId the ID of the project
   * @return list of {@link VelocityPointDTO} one per cycle
   */
  public List<VelocityPointDTO> computeVelocity(Long projectId) {
    List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);

    // Reverse to return oldest-first for a natural velocity chart
    List<Cycle> orderedCycles = cycles.reversed();

    return orderedCycles.stream()
        .map(
            cycle -> {
              List<Task> tasks = taskRepository.findByCycleId(cycle.getId());

              int planned =
                  tasks.stream()
                      .filter(t -> t.getStoryPoints() != null)
                      .mapToInt(Task::getStoryPoints)
                      .sum();

              int completed =
                  tasks.stream()
                      .filter(
                          t ->
                              t.getStoryPoints() != null && t.getStatus() == TaskStatus.DONE)
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
