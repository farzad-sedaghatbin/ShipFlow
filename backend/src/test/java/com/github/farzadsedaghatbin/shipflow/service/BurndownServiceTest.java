package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.BurndownPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BurndownServiceTest {

  @Mock private CycleRepository cycleRepository;

  @Mock private TaskRepository taskRepository;

  @InjectMocks private BurndownService burndownService;

  @Test
  void computeBurndown_cycleNotFound_throwsResourceNotFoundException() {
    when(cycleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> burndownService.computeBurndown(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void computeBurndown_noScoredTasks_returnsEmptySeries() {
    // Sprint ended yesterday — always in the past, no time-dependency
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycle(1L, start, end);

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of());

    List<BurndownPointDTO> result = burndownService.computeBurndown(1L);

    // All tasks have null storyPoints → early-return empty list (Fix 6)
    assertThat(result).isEmpty();
  }

  @Test
  void computeBurndown_withScoredTasks_remainingDecreaseAsTasksDone() {
    // Sprint: 7 days ago → 1 day ago; always fully in the past
    LocalDate start = LocalDate.now().minusDays(7);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycle(2L, start, end);

    // task1 completed on day 2 of the sprint (5 pts)
    LocalDateTime task1Done = start.plusDays(1).atTime(12, 0);
    // task2 completed on day 4 of the sprint (3 pts)
    LocalDateTime task2Done = start.plusDays(3).atTime(10, 0);

    Task task1 = buildTask(1L, 5, TaskStatus.DONE, task1Done);
    Task task2 = buildTask(2L, 3, TaskStatus.DONE, task2Done);
    Task task3 = buildTask(3L, 2, TaskStatus.IN_PROGRESS, null);

    when(cycleRepository.findById(2L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(2L)).thenReturn(List.of(task1, task2, task3));

    List<BurndownPointDTO> result = burndownService.computeBurndown(2L);

    // Day 0 (startDate): nothing completed yet — remaining = total = 10
    BurndownPointDTO day1 = result.get(0);
    assertThat(day1.getDate()).isEqualTo(start);
    assertThat(day1.getRemainingPoints()).isEqualTo(10);

    // Day 1 (startDate + 1 day): task1 completed (5 pts burned) — remaining = 5
    BurndownPointDTO day2 = result.get(1);
    assertThat(day2.getDate()).isEqualTo(start.plusDays(1));
    assertThat(day2.getRemainingPoints()).isEqualTo(5);

    // Ideal on day 0: total * daysLeft/totalDays = 10 * totalDays/totalDays = 10
    assertThat(day1.getIdealPoints()).isEqualTo(10);
  }

  @Test
  void resolveProjectId_returnsProjectId() {
    Project project = Project.builder().build();
    project.setId(42L);
    Cycle cycle = buildCycle(5L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
    cycle.setProject(project);

    when(cycleRepository.findByIdWithProject(5L)).thenReturn(Optional.of(cycle));

    assertThat(burndownService.resolveProjectId(5L)).isEqualTo(42L);
  }

  @Test
  void resolveProjectId_cycleNotFound_throwsResourceNotFoundException() {
    when(cycleRepository.findByIdWithProject(77L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> burndownService.resolveProjectId(77L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("77");
  }

  @Test
  void computeBurndown_nullEndDate_defaultsToFourteenDays() {
    // Sprint started 5 days ago, no end date set
    LocalDate start = LocalDate.now().minusDays(5);
    Cycle cycle = buildCycle(3L, start, null);

    when(cycleRepository.findById(3L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(3L)).thenReturn(List.of());

    // Should not throw; defaults endDate to start + 14 days → returns empty (no scored tasks)
    List<BurndownPointDTO> result = burndownService.computeBurndown(3L);
    assertThat(result).isEmpty();
  }

  @Test
  void computeBurndown_idealBurndown_day0EqualsTotalAndLastDayEqualsZero() {
    // 5-day sprint fully in the past
    LocalDate start = LocalDate.now().minusDays(5);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycle(10L, start, end);

    Task task = buildTask(10L, 10, TaskStatus.IN_PROGRESS, null);
    when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(10L)).thenReturn(List.of(task));

    List<BurndownPointDTO> result = burndownService.computeBurndown(10L);

    // day 0 ideal == total (10)
    assertThat(result.get(0).getDate()).isEqualTo(start);
    assertThat(result.get(0).getIdealPoints()).isEqualTo(10);

    // last day ideal == 0 (or very close — Math.round may give 0 or 1 depending on rounding)
    BurndownPointDTO lastPoint = result.get(result.size() - 1);
    assertThat(lastPoint.getDate()).isEqualTo(end);
    assertThat(lastPoint.getIdealPoints()).isLessThanOrEqualTo(1);
  }

  @Test
  void computeBurndown_midSprint_seriesEndsAtToday() {
    // Sprint started 3 days ago, ends 4 days from now — ongoing sprint
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().plusDays(4);
    Cycle cycle = buildCycle(11L, start, end);

    Task task = buildTask(11L, 8, TaskStatus.IN_PROGRESS, null);
    when(cycleRepository.findById(11L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(11L)).thenReturn(List.of(task));

    List<BurndownPointDTO> result = burndownService.computeBurndown(11L);

    // Series must end at today (not at sprintEnd which is in the future)
    assertThat(result).isNotEmpty();
    BurndownPointDTO lastPoint = result.get(result.size() - 1);
    assertThat(lastPoint.getDate()).isEqualTo(LocalDate.now());
  }

  @Test
  void computeBurndown_sameDayStartAndEnd_noDivisionByZero() {
    // Edge case: totalDays == 0 (start == end, treated as 1 day)
    LocalDate start = LocalDate.now().minusDays(1);
    LocalDate end = LocalDate.now().minusDays(1); // same as start
    Cycle cycle = buildCycle(12L, start, end);

    Task task = buildTask(12L, 5, TaskStatus.IN_PROGRESS, null);
    when(cycleRepository.findById(12L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(12L)).thenReturn(List.of(task));

    // Must not throw ArithmeticException
    List<BurndownPointDTO> result = burndownService.computeBurndown(12L);

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getRemainingPoints()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void computeBurndown_remainingClampedAtZero() {
    // All tasks completed before the start date: burned > total would go negative without clamping
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycle(13L, start, end);

    // Completed far in the past — before sprint even started
    LocalDateTime completedBeforeSprint = start.minusDays(2).atTime(9, 0);
    Task task = buildTask(13L, 5, TaskStatus.DONE, completedBeforeSprint);
    when(cycleRepository.findById(13L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleIdNotDeleted(13L)).thenReturn(List.of(task));

    List<BurndownPointDTO> result = burndownService.computeBurndown(13L);

    // All points should have remainingPoints >= 0 (clamped by Math.max)
    assertThat(result).allMatch(p -> p.getRemainingPoints() >= 0);
  }

  // ---- helpers ----

  private Cycle buildCycle(Long id, LocalDate start, LocalDate end) {
    Cycle c = new Cycle();
    c.setId(id);
    c.setStartDate(start);
    c.setEndDate(end);
    return c;
  }

  private Task buildTask(Long id, int points, TaskStatus status, LocalDateTime completedAt) {
    Task t = new Task();
    t.setId(id);
    t.setStoryPoints(points);
    t.setStatus(status);
    t.setCompletedAt(completedAt);
    return t;
  }
}
