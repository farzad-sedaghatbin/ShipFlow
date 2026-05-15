package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.VelocityPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VelocityServiceTest {

  @Mock private CycleRepository cycleRepository;

  @Mock private TaskRepository taskRepository;

  @InjectMocks private VelocityService velocityService;

  @Test
  void computeVelocity_noCycles_returnsEmptyList() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of());

    List<VelocityPointDTO> result = velocityService.computeVelocity(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void computeVelocity_multipleCycles_returnedOldestFirst() {
    Cycle older = buildCycle(1L, "Sprint 1", LocalDate.of(2026, 1, 1));
    Cycle newer = buildCycle(2L, "Sprint 2", LocalDate.of(2026, 2, 1));

    // Repository returns newest first (DESC)
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(10L))
        .thenReturn(List.of(newer, older));
    when(taskRepository.findByCycleId(1L)).thenReturn(List.of());
    when(taskRepository.findByCycleId(2L)).thenReturn(List.of());

    List<VelocityPointDTO> result = velocityService.computeVelocity(10L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCycleName()).isEqualTo("Sprint 1");
    assertThat(result.get(1).getCycleName()).isEqualTo("Sprint 2");
  }

  @Test
  void computeVelocity_tasksWithPoints_correctPlannedAndCompleted() {
    Cycle cycle = buildCycle(5L, "Sprint 5", LocalDate.of(2026, 3, 1));

    Task done1 = buildTask(1L, 5, TaskStatus.DONE);
    Task done2 = buildTask(2L, 3, TaskStatus.DONE);
    Task inProgress = buildTask(3L, 8, TaskStatus.IN_PROGRESS);
    Task noPoints = buildTask(4L, null, TaskStatus.DONE);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(20L)).thenReturn(List.of(cycle));
    when(taskRepository.findByCycleId(5L)).thenReturn(List.of(done1, done2, inProgress, noPoints));

    List<VelocityPointDTO> result = velocityService.computeVelocity(20L);

    assertThat(result).hasSize(1);
    VelocityPointDTO point = result.get(0);
    assertThat(point.getCycleId()).isEqualTo(5L);
    assertThat(point.getPlannedPoints()).isEqualTo(16); // 5+3+8, noPoints excluded
    assertThat(point.getCompletedPoints()).isEqualTo(8); // 5+3 (done1+done2 only)
  }

  // ---- helpers ----

  private Cycle buildCycle(Long id, String name, LocalDate start) {
    Cycle c = new Cycle();
    c.setId(id);
    c.setName(name);
    c.setStartDate(start);
    c.setEndDate(start.plusDays(13));
    return c;
  }

  private Task buildTask(Long id, Integer points, TaskStatus status) {
    Task t = new Task();
    t.setId(id);
    t.setStoryPoints(points);
    t.setStatus(status);
    return t;
  }
}
