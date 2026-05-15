package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.BurndownPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
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
  void computeBurndown_noScoredTasks_returnsZeroSeries() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 3);
    Cycle cycle = buildCycle(1L, start, end);

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleId(1L)).thenReturn(List.of());

    List<BurndownPointDTO> result = burndownService.computeBurndown(1L);

    // Series should cover start to min(today, end) — for this test end is in the past
    // so series covers start through end (3 days)
    assertThat(result).isNotEmpty();
    result.forEach(
        point -> {
          assertThat(point.getRemainingPoints()).isZero();
          assertThat(point.getIdealPoints()).isZero();
        });
  }

  @Test
  void computeBurndown_withScoredTasks_remainingDecreaseAsTasksDone() {
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 1, 5);
    Cycle cycle = buildCycle(2L, start, end);

    Task task1 = buildTask(1L, 5, TaskStatus.DONE, LocalDateTime.of(2026, 1, 2, 12, 0));
    Task task2 = buildTask(2L, 3, TaskStatus.DONE, LocalDateTime.of(2026, 1, 4, 10, 0));
    Task task3 = buildTask(3L, 2, TaskStatus.IN_PROGRESS, null);

    when(cycleRepository.findById(2L)).thenReturn(Optional.of(cycle));
    when(taskRepository.findByCycleId(2L)).thenReturn(List.of(task1, task2, task3));

    List<BurndownPointDTO> result = burndownService.computeBurndown(2L);

    // total = 10; on day 1 (Jan 1) nothing is done yet
    BurndownPointDTO day1 = result.get(0);
    assertThat(day1.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(day1.getRemainingPoints()).isEqualTo(10);

    // On day 2 (Jan 2) task1 completed (5 pts burned)
    BurndownPointDTO day2 = result.get(1);
    assertThat(day2.getDate()).isEqualTo(LocalDate.of(2026, 1, 2));
    assertThat(day2.getRemainingPoints()).isEqualTo(5);

    // Ideal on day 1: total * daysLeft/totalDays = 10 * 4/4 = 10
    assertThat(day1.getIdealPoints()).isEqualTo(10);
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
