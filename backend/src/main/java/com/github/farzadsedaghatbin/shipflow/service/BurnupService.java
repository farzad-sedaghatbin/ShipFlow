package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.BurnupPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Computes burnup chart data for a Scrum sprint (cycle). Each point on the chart represents the
 * cumulative completed story points for a given day from the sprint start to the lesser of today
 * or the sprint end date, alongside the total scope of the sprint (constant across the series).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BurnupService {

  private final TaskRepository taskRepository;

  /**
   * Compute the burnup series for the given cycle. The caller (BurnupController) is responsible
   * for loading the cycle and enforcing project-scope authorization before calling this method.
   * Accepting the pre-loaded {@link Cycle} avoids a redundant DB query.
   *
   * @param cycle the sprint/cycle entity (must not be null)
   * @return ordered list of {@link BurnupPointDTO} from startDate to min(today, endDate)
   */
  public List<BurnupPointDTO> computeBurnup(Cycle cycle) {
    Long cycleId = cycle.getId();

    if (cycle.getProject() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cycle " + cycleId + " has no associated project");
    }

    LocalDate startDate = cycle.getStartDate();
    if (startDate == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cycle " + cycleId + " has no start date");
    }

    LocalDate endDate = cycle.getEndDate();
    // Default to a 2-week sprint when no end date is configured
    if (endDate == null) {
      log.warn(
          "Cycle {} has no end date; defaulting to startDate + 14 days for burnup computation",
          cycleId);
      endDate = startDate.plusDays(14);
    }

    LocalDate today = LocalDate.now();
    LocalDate seriesEnd = today.isBefore(endDate) ? today : endDate;

    // All tasks in the cycle that have story points (soft-delete aware)
    List<Task> tasks = taskRepository.findByCycleIdNotDeleted(cycleId);

    log.info(
        "BurnupService: cycleId={} tasks={} storyPointed={}",
        cycleId,
        tasks.size(),
        tasks.stream().filter(t -> t.getStoryPoints() != null).count());

    // Early return when no tasks exist or none carry story points (same convention as
    // BurndownService)
    if (tasks.isEmpty() || tasks.stream().allMatch(t -> t.getStoryPoints() == null)) {
      log.warn(
          "BurnupService: returning empty series for cycleId={} — no tasks with story points",
          cycleId);
      return List.of();
    }

    List<Task> scoredTasks = tasks.stream().filter(t -> t.getStoryPoints() != null).toList();

    int total = scoredTasks.stream().mapToInt(Task::getStoryPoints).sum();

    List<BurnupPointDTO> series = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(seriesEnd); date = date.plusDays(1)) {
      final LocalDate pointDate = date;

      // Points completed = story points of tasks whose date of completion is on or before this
      // day. Using toLocalDate() avoids the midnight boundary off-by-one, matching
      // BurndownService's "burned" computation.
      int completed =
          scoredTasks.stream()
              .filter(
                  t ->
                      t.getStatus() == TaskStatus.DONE
                          && t.getCompletedAt() != null
                          && !t.getCompletedAt().toLocalDate().isAfter(pointDate))
              .mapToInt(Task::getStoryPoints)
              .sum();

      series.add(
          BurnupPointDTO.builder()
              .date(pointDate)
              .completedPoints(completed)
              .totalScopePoints(total)
              .build());
    }

    return series;
  }
}
