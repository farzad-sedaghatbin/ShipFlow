package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.SprintReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
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
class SprintReportServiceTest {

  @Mock private TaskRepository taskRepository;

  @InjectMocks private SprintReportService sprintReportService;

  @Test
  void computeSprintReport_noStartDate_throwsBadRequest() {
    Cycle cycle = new Cycle();
    cycle.setId(1L);
    cycle.setStartDate(null);

    assertThatThrownBy(() -> sprintReportService.computeSprintReport(cycle))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void computeSprintReport_noTasks_zeroPlannedPoints_completionRateIsZero() {
    LocalDate start = LocalDate.now().minusDays(3);
    Cycle cycle = buildCycle(1L, "Sprint 1", start, start.plusDays(11));

    when(taskRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of());

    SprintReportDTO result = sprintReportService.computeSprintReport(cycle);

    // Divide-by-zero guard: plannedPoints == 0 must not blow up, completionRate must be 0.0
    assertThat(result.getPlannedPoints()).isEqualTo(0);
    assertThat(result.getCompletedPoints()).isEqualTo(0);
    assertThat(result.getCompletionRate()).isEqualTo(0.0);
    assertThat(result.getScopeAddedTaskCount()).isEqualTo(0);
    assertThat(result.getScopeAddedPoints()).isEqualTo(0);
    // All 7 TaskStatus values are present even with zero tasks
    assertThat(result.getTaskCountByStatus()).hasSize(7);
    assertThat(result.getTaskCountByStatus().values()).allMatch(v -> v == 0);
  }

  @Test
  void computeSprintReport_tasksWithPoints_correctPlannedCompletedAndRate() {
    LocalDate start = LocalDate.now().minusDays(5);
    Cycle cycle = buildCycle(2L, "Sprint 2", start, start.plusDays(13));

    Task done1 = buildTask(1L, 5, TaskStatus.DONE, start.minusDays(1).atStartOfDay());
    Task done2 = buildTask(2L, 3, TaskStatus.DONE, start.minusDays(1).atStartOfDay());
    Task inProgress = buildTask(3L, 8, TaskStatus.IN_PROGRESS, start.minusDays(1).atStartOfDay());
    Task noPoints = buildTask(4L, null, TaskStatus.TODO, start.minusDays(1).atStartOfDay());

    when(taskRepository.findByCycleIdNotDeleted(2L))
        .thenReturn(List.of(done1, done2, inProgress, noPoints));

    SprintReportDTO result = sprintReportService.computeSprintReport(cycle);

    assertThat(result.getPlannedPoints()).isEqualTo(16); // 5+3+8, noPoints excluded
    assertThat(result.getCompletedPoints()).isEqualTo(8); // 5+3
    assertThat(result.getCompletionRate()).isEqualTo(8.0 / 16.0);

    assertThat(result.getTaskCountByStatus().get("DONE")).isEqualTo(2);
    assertThat(result.getTaskCountByStatus().get("IN_PROGRESS")).isEqualTo(1);
    assertThat(result.getTaskCountByStatus().get("TODO")).isEqualTo(1);
    assertThat(result.getTaskCountByStatus().get("BACKLOG")).isEqualTo(0);
  }

  @Test
  void computeSprintReport_taskCreatedAfterStartDate_countsAsScopeAdded() {
    LocalDate start = LocalDate.now().minusDays(5);
    Cycle cycle = buildCycle(3L, "Sprint 3", start, start.plusDays(13));

    // Created before the sprint started — not scope-added
    Task original = buildTask(1L, 5, TaskStatus.TODO, null);
    original.setCreatedAt(start.minusDays(1).atTime(9, 0));

    // Created after the sprint started — scope-added
    Task addedMidSprint = buildTask(2L, 3, TaskStatus.TODO, null);
    addedMidSprint.setCreatedAt(start.plusDays(2).atTime(9, 0));

    when(taskRepository.findByCycleIdNotDeleted(3L)).thenReturn(List.of(original, addedMidSprint));

    SprintReportDTO result = sprintReportService.computeSprintReport(cycle);

    assertThat(result.getScopeAddedTaskCount()).isEqualTo(1);
    assertThat(result.getScopeAddedPoints()).isEqualTo(3);
  }

  // ---- helpers ----

  private Cycle buildCycle(Long id, String name, LocalDate start, LocalDate end) {
    Cycle c = new Cycle();
    c.setId(id);
    c.setName(name);
    c.setStartDate(start);
    c.setEndDate(end);
    return c;
  }

  private Task buildTask(Long id, Integer points, TaskStatus status, LocalDateTime createdAt) {
    Task t = new Task();
    t.setId(id);
    t.setStoryPoints(points);
    t.setStatus(status);
    t.setCreatedAt(createdAt);
    return t;
  }
}
