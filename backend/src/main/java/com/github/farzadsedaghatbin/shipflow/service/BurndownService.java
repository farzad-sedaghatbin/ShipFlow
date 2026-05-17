package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.BurndownPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Computes burndown chart data for a Scrum sprint (cycle). Each point on the chart represents the
 * remaining story points for a given day from the sprint start to the lesser of today or the sprint
 * end date. An ideal linear burndown line is also included.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BurndownService {

  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;

  /**
   * Compute the burndown series for the given cycle. Project-scope authorization is enforced by
   * {@code BurndownController} before this method is called.
   *
   * @param cycleId the ID of the sprint/cycle
   * @return ordered list of {@link BurndownPointDTO} from startDate to min(today, endDate)
   */
  public List<BurndownPointDTO> computeBurndown(Long cycleId) {
    Cycle cycle =
        cycleRepository
            .findByIdWithProject(cycleId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

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
          "Cycle {} has no end date; defaulting to startDate + 14 days for burndown computation",
          cycleId);
      endDate = startDate.plusDays(14);
    }

    LocalDate today = LocalDate.now();
    LocalDate seriesEnd = today.isBefore(endDate) ? today : endDate;

    // All tasks in the cycle that have story points (soft-delete aware)
    List<Task> tasks = taskRepository.findByCycleIdNotDeleted(cycleId);

    // Early return when no tasks carry story points (tasks with storyPoints == 0 still
    // produce a series with 0 total, matching VelocityService which treats 0 as planned)
    if (tasks.stream().allMatch(t -> t.getStoryPoints() == null)) {
      return List.of();
    }

    List<Task> scoredTasks =
        tasks.stream()
            .filter(t -> t.getStoryPoints() != null)
            .toList();

    int total = scoredTasks.stream().mapToInt(Task::getStoryPoints).sum();
    // ChronoUnit.DAYS.between gives total elapsed days, not just the days component of a Period.
    // startDate.until(endDate).getDays() is wrong for cross-month spans (e.g. Apr 15 → May 15 = 0).
    long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
    if (totalDays <= 0) {
      totalDays = 1;
    }

    List<BurndownPointDTO> series = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(seriesEnd); date = date.plusDays(1)) {
      final LocalDate pointDate = date;

      // Points burned = story points of tasks completed on or before this day
      int burned =
          scoredTasks.stream()
              .filter(
                  t ->
                      t.getStatus() == TaskStatus.DONE
                          && t.getCompletedAt() != null
                          && !t.getCompletedAt()
                              .isAfter(
                                  LocalDateTime.of(
                                      pointDate.plusDays(1), java.time.LocalTime.MIDNIGHT)))
              .mapToInt(Task::getStoryPoints)
              .sum();

      int remaining = total - burned;

      // Ideal linear burndown: total * (daysLeft / totalDays)
      long daysElapsed = ChronoUnit.DAYS.between(startDate, pointDate);
      long daysLeft = totalDays - daysElapsed;
      int ideal = (int) Math.round((double) total * daysLeft / totalDays);

      series.add(
          BurndownPointDTO.builder()
              .date(pointDate)
              .remainingPoints(Math.max(remaining, 0))
              .idealPoints(Math.max(ideal, 0))
              .build());
    }

    return series;
  }
}
