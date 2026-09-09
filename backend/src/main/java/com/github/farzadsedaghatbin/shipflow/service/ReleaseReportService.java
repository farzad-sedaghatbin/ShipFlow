package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.ReleaseReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes release report data across all releases for a project. For each release, reports task
 * counts and planned/completed story points among the tasks that target it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReleaseReportService {

  private final ReleaseRepository releaseRepository;
  private final TaskRepository taskRepository;

  /**
   * Compute the release report for every release in the project.
   *
   * <p>Uses a single batch query for all project tasks (grouped in memory by target release) to
   * avoid N+1 queries (one per release), mirroring {@code VelocityService}'s pattern for cycles.
   *
   * @param projectId the ID of the project
   * @return list of {@link ReleaseReportDTO}, one per release
   */
  public List<ReleaseReportDTO> computeReleaseReport(Long projectId) {
    List<Release> releases = releaseRepository.findByProjectIdNotDeleted(projectId);

    // Single batch query for all non-deleted tasks in the project — avoids N+1 (one query per
    // release)
    List<Task> allTasks = taskRepository.findByProjectIdNotDeleted(projectId);
    Map<Long, List<Task>> byRelease =
        allTasks.stream()
            .filter(t -> t.getTargetRelease() != null)
            .collect(Collectors.groupingBy(t -> t.getTargetRelease().getId()));

    log.info(
        "ReleaseReportService: projectId={} releases={} tasks={}",
        projectId,
        releases.size(),
        allTasks.size());

    return releases.stream()
        .map(
            release -> {
              List<Task> tasks = byRelease.getOrDefault(release.getId(), List.of());

              int taskCount = tasks.size();
              int completedTaskCount =
                  (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

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

              return ReleaseReportDTO.builder()
                  .releaseId(release.getId())
                  .releaseName(release.getName())
                  .version(release.getVersion())
                  .status(release.getStatus())
                  .targetDate(release.getTargetDate())
                  .releaseDate(release.getReleaseDate())
                  .taskCount(taskCount)
                  .completedTaskCount(completedTaskCount)
                  .plannedPoints(plannedPoints)
                  .completedPoints(completedPoints)
                  .build();
            })
        .toList();
  }
}
