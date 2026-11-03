package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.PitchHealthService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CycleHealthBuilder")
class CycleHealthBuilderTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

  @Mock private CycleRepository cycleRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private ObjectProvider<PitchHealthService> pitchHealth;
  @Mock private PitchHealthService pitchHealthService;

  private CycleHealthBuilder builder;
  private Project project;
  private Cycle cycle;
  private final AgentLoopConfig config = AgentLoopConfig.parse("{}", new ObjectMapper());

  @BeforeEach
  void setUp() {
    builder = new CycleHealthBuilder(cycleRepository, taskRepository, pitchHealth);
    project = new Project();
    project.setId(5L);
    project.setName("Payments");
    project.setProjectType(ProjectType.SCRUM);
    cycle = new Cycle();
    cycle.setId(9L);
    cycle.setName("Sprint 4");
    cycle.setStartDate(LocalDate.of(2026, 11, 2));
    cycle.setEndDate(LocalDate.of(2026, 11, 30)); // 28-day span; TODAY is day 14 → 50% elapsed
    when(cycleRepository.findByProjectIdAndIsActiveTrue(5L)).thenReturn(List.of(cycle));
    when(pitchHealth.getIfAvailable()).thenReturn(pitchHealthService);
  }

  private static List<Task> tasks(int done, int open) {
    return java.util.stream.Stream.concat(
        IntStream.range(0, done).mapToObj(i -> task("Done " + i, TaskStatus.DONE)),
        IntStream.range(0, open).mapToObj(i -> task("Open " + i, TaskStatus.IN_PROGRESS))).toList();
  }

  private static Task task(String title, TaskStatus status) {
    Task t = new Task();
    t.setTitle(title);
    t.setStatus(status);
    return t;
  }

  @Test
  @DisplayName("No active cycle: a plain statement, no findings")
  void noCycle() {
    when(cycleRepository.findByProjectIdAndIsActiveTrue(5L)).thenReturn(List.of());

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).contains("no active cycle");
  }

  @Test
  @DisplayName("Completion trailing elapsed time by more than 15 points is 'behind schedule', with the real numbers")
  void behindSchedule() {
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(2, 8)); // 20% done vs 50% elapsed

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.facts())
        .startsWith("⚠️ At risk")
        .contains("Sprint 4 (2026-11-02 → 2026-11-30)")
        .contains("2 of 10 tasks done (20%), 50% of the time elapsed")
        .contains("Behind schedule: 20% done vs 50% of the time gone");
    assertThat(r.entityType()).isEqualTo("CYCLE");
    assertThat(r.entityId()).isEqualTo(9L);
  }

  @Test
  @DisplayName("Completion within the tolerance is on track and quiet")
  void onTrack() {
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(4, 6)); // 40% vs 50%: within 15 points

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).startsWith("✅ On track").contains("No risks found.");
  }

  @Test
  @DisplayName("Blocked and overdue tasks are findings even when the schedule is fine")
  void blockedAndOverdue() {
    Task blocked = task("Deploy", TaskStatus.BLOCKED);
    Task overdue = task("Invoice", TaskStatus.TODO);
    overdue.setDueDate(TODAY.minusDays(3));
    List<Task> all = new java.util.ArrayList<>(tasks(5, 3));
    all.add(blocked);
    all.add(overdue);
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(all);

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.facts()).contains("1 blocked: Deploy").contains("1 overdue: Invoice");
  }

  @Test
  @DisplayName("Cancelled tasks don't count towards progress")
  void cancelledIgnored() {
    List<Task> all = new java.util.ArrayList<>(tasks(4, 6));
    all.add(task("Dropped", TaskStatus.CANCELLED));
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(all);

    assertThat(builder.build(project, config, TODAY).facts()).contains("4 of 10 tasks done");
  }

  @Test
  @DisplayName("A cycle that has ended with open work is flagged")
  void endedWithOpenWork() {
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(9, 1));

    AgentLoopReport r = builder.build(project, config, LocalDate.of(2026, 12, 2));

    assertThat(r.facts()).contains("The cycle ended 2026-11-30 with 1 task(s) still open");
  }

  @Test
  @DisplayName("A cycle without dates is judged only on blocked/overdue work, never on elapsed time")
  void undatedCycle() {
    cycle.setStartDate(null);
    cycle.setEndDate(null);
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(0, 10));

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).doesNotContain("elapsed").doesNotContain("Behind schedule");
  }

  @Test
  @DisplayName("Shape Up: pitches the health service rates HIGH or CRITICAL are reported; others are not")
  void shapeUpPitchRisk() {
    project.setProjectType(ProjectType.SHAPE_UP);
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(5, 5));
    PitchHealthDTO critical = PitchHealthDTO.builder().pitchName("Instant Transfer UI").riskLevel(RiskLevel.CRITICAL).build();
    PitchHealthDTO fine = PitchHealthDTO.builder().pitchName("Dashboard").riskLevel(RiskLevel.LOW).build();
    when(pitchHealthService.getCycleHealthSummary(anyLong(), anyBoolean()))
        .thenReturn(CycleHealthSummaryDTO.builder().pitchHealthList(List.of(critical, fine)).build());

    AgentLoopReport r = builder.build(project, config, TODAY);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.facts()).contains("1 high-risk pitch(es): Instant Transfer UI (CRITICAL)").doesNotContain("Dashboard");
  }

  @Test
  @DisplayName("Non-Shape-Up projects never consult pitch health, and a health-service failure is not fatal")
  void pitchHealthOptional() {
    when(taskRepository.findByCycleIdWithAssignee(9L)).thenReturn(tasks(5, 5));
    assertThat(builder.build(project, config, TODAY).hasFindings()).isFalse();

    project.setProjectType(ProjectType.SHAPE_UP);
    when(pitchHealthService.getCycleHealthSummary(anyLong(), anyBoolean())).thenThrow(new RuntimeException("boom"));
    assertThat(builder.build(project, config, TODAY).hasFindings()).isFalse();
  }
}
