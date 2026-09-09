package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.BurnupPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BurnupServiceTest {

  @Mock private TaskRepository taskRepository;

  @InjectMocks private BurnupService burnupService;

  @Test
  void computeBurnup_noProjectOnCycle_throwsBadRequest() {
    Cycle cycle = new Cycle();
    cycle.setId(1L);
    cycle.setProject(null);
    cycle.setStartDate(LocalDate.now().minusDays(3));

    assertThatThrownBy(() -> burnupService.computeBurnup(cycle))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void computeBurnup_noScoredTasks_returnsEmptySeries() {
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycleWithProject(1L, start, end, 5L);

    when(taskRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of());

    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);

    assertThat(result).isEmpty();
  }

  @Test
  void computeBurnup_allTasksHaveNullStoryPoints_returnsEmptySeries() {
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycleWithProject(20L, start, end, 5L);

    Task taskNoPoints1 = new Task();
    taskNoPoints1.setId(201L);
    taskNoPoints1.setStoryPoints(null);
    taskNoPoints1.setStatus(TaskStatus.IN_PROGRESS);

    Task taskNoPoints2 = new Task();
    taskNoPoints2.setId(202L);
    taskNoPoints2.setStoryPoints(null);
    taskNoPoints2.setStatus(TaskStatus.DONE);

    when(taskRepository.findByCycleIdNotDeleted(20L))
        .thenReturn(List.of(taskNoPoints1, taskNoPoints2));

    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);

    assertThat(result).isEmpty();
  }

  @Test
  void computeBurnup_withScoredTasks_completedIncreasesAsTasksDone() {
    // Sprint: 7 days ago → 1 day ago; always fully in the past
    LocalDate start = LocalDate.now().minusDays(7);
    LocalDate end = LocalDate.now().minusDays(1);
    Cycle cycle = buildCycleWithProject(2L, start, end, 5L);

    // task1 completed on day 2 of the sprint (5 pts)
    LocalDateTime task1Done = start.plusDays(1).atTime(12, 0);
    // task2 completed on day 4 of the sprint (3 pts)
    LocalDateTime task2Done = start.plusDays(3).atTime(10, 0);

    Task task1 = buildTask(1L, 5, TaskStatus.DONE, task1Done);
    Task task2 = buildTask(2L, 3, TaskStatus.DONE, task2Done);
    Task task3 = buildTask(3L, 2, TaskStatus.IN_PROGRESS, null);

    when(taskRepository.findByCycleIdNotDeleted(2L)).thenReturn(List.of(task1, task2, task3));

    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);

    // Day 0 (startDate): nothing completed yet — completed = 0, total scope = 10
    BurnupPointDTO day1 = result.get(0);
    assertThat(day1.getDate()).isEqualTo(start);
    assertThat(day1.getCompletedPoints()).isEqualTo(0);
    assertThat(day1.getTotalScopePoints()).isEqualTo(10);

    // Day 1 (startDate + 1 day): task1 completed (5 pts) — completed = 5
    BurnupPointDTO day2 = result.get(1);
    assertThat(day2.getDate()).isEqualTo(start.plusDays(1));
    assertThat(day2.getCompletedPoints()).isEqualTo(5);
    assertThat(day2.getTotalScopePoints()).isEqualTo(10);

    // Day 3 (startDate + 3 days): both tasks completed — completed = 8
    BurnupPointDTO day4 = result.get(3);
    assertThat(day4.getDate()).isEqualTo(start.plusDays(3));
    assertThat(day4.getCompletedPoints()).isEqualTo(8);

    // Total scope stays constant across the entire series
    assertThat(result).allMatch(p -> p.getTotalScopePoints() == 10);
  }

  @Test
  void computeBurnup_nullEndDate_defaultsToFourteenDays() {
    LocalDate start = LocalDate.now().minusDays(5);
    Cycle cycle = buildCycleWithProject(3L, start, null, 5L);

    when(taskRepository.findByCycleIdNotDeleted(3L)).thenReturn(List.of());

    // Should not throw; defaults endDate to start + 14 days → returns empty (no scored tasks)
    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);
    assertThat(result).isEmpty();
  }

  @Test
  void computeBurnup_nullEndDate_withScoredTasks_seriesCappedAtToday() {
    // No end date + start far enough in the past that start+14 would be in the future — the
    // series must still be capped at today, not run all the way to start+14.
    LocalDate start = LocalDate.now().minusDays(5);
    Cycle cycle = buildCycleWithProject(4L, start, null, 5L);

    Task task = buildTask(4L, 8, TaskStatus.IN_PROGRESS, null);
    when(taskRepository.findByCycleIdNotDeleted(4L)).thenReturn(List.of(task));

    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);

    assertThat(result).isNotEmpty();
    BurnupPointDTO lastPoint = result.get(result.size() - 1);
    assertThat(lastPoint.getDate()).isEqualTo(LocalDate.now());
  }

  @Test
  void computeBurnup_midSprint_seriesEndsAtToday() {
    LocalDate start = LocalDate.now().minusDays(3);
    LocalDate end = LocalDate.now().plusDays(4);
    Cycle cycle = buildCycleWithProject(11L, start, end, 5L);

    Task task = buildTask(11L, 8, TaskStatus.IN_PROGRESS, null);
    when(taskRepository.findByCycleIdNotDeleted(11L)).thenReturn(List.of(task));

    List<BurnupPointDTO> result = burnupService.computeBurnup(cycle);

    assertThat(result).isNotEmpty();
    BurnupPointDTO lastPoint = result.get(result.size() - 1);
    assertThat(lastPoint.getDate()).isEqualTo(LocalDate.now());
  }

  // ---- helpers ----

  private Cycle buildCycleWithProject(Long id, LocalDate start, LocalDate end, Long projectId) {
    Project project = Project.builder().build();
    project.setId(projectId);
    Cycle c = new Cycle();
    c.setId(id);
    c.setStartDate(start);
    c.setEndDate(end);
    c.setProject(project);
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
