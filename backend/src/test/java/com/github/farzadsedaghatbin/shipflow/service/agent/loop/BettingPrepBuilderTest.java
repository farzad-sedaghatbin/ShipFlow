package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.betting.CapacityAnalysisDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.BettingTableService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BettingPrepBuilder")
class BettingPrepBuilderTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

  @Mock private CycleRepository cycleRepository;
  @Mock private BettingTableService bettingTableService;

  private BettingPrepBuilder builder;
  private Project project;

  @BeforeEach
  void setUp() {
    builder = new BettingPrepBuilder(cycleRepository, bettingTableService);
    project = new Project();
    project.setId(5L);
    project.setName("Payments");
    project.setProjectType(ProjectType.SHAPE_UP);
  }

  private static Cycle cycle(long id, String name, LocalDate start, LocalDate end) {
    Cycle c = new Cycle();
    c.setId(id);
    c.setName(name);
    c.setStartDate(start);
    c.setEndDate(end);
    return c;
  }

  private static PitchDTO pitch(String title, Integer days) {
    return PitchDTO.builder().title(title).appetiteDays(days).build();
  }

  @Test
  @DisplayName("Only applies to Shape Up: other methodologies get a plain note and no betting-table calls")
  void shapeUpOnly() {
    project.setProjectType(ProjectType.KANBAN);

    AgentLoopReport r = builder.build(project, TODAY);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).contains("only applies to Shape Up");
    verify(bettingTableService, never()).getShapedPitchesForBetting(anyLong());
  }

  @Test
  @DisplayName("Lists shaped pitches with appetite, the next cycle, its capacity and capacity warnings")
  void fullBrief() {
    when(bettingTableService.getShapedPitchesForBetting(5L))
        .thenReturn(List.of(pitch("Instant Transfer UI", 14), pitch("Fraud Alerts", 10)));
    // Past cycle is ignored; the earliest cycle starting today-or-later is the target.
    when(cycleRepository.findByProjectIdOrderByStartDateAsc(5L)).thenReturn(List.of(
        cycle(1L, "Old", TODAY.minusDays(60), TODAY.minusDays(18)),
        cycle(2L, "Cycle 7", TODAY.plusDays(14), TODAY.plusDays(56))));
    when(bettingTableService.getCapacityAnalysis(2L)).thenReturn(CapacityAnalysisDTO.builder()
        .totalCapacityWeeks(12).allocatedWeeks(9).remainingWeeks(3)
        .warnings(List.of(CapacityAnalysisDTO.CapacityWarning.builder().message("Team Alpha is over-allocated").build()))
        .build());

    AgentLoopReport r = builder.build(project, TODAY);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.facts())
        .contains("Next cycle: Cycle 7")
        .contains("Capacity: 9 of 12 team-weeks already allocated, 3 left")
        .contains("⚠ Team Alpha is over-allocated")
        .contains("🎯 Shaped pitches ready to bet on (2)")
        .contains("Instant Transfer UI — 14 days appetite")
        .contains("Total appetite across them: 24 days");
    assertThat(r.entityType()).isEqualTo("CYCLE");
    assertThat(r.entityId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("With no shaped pitches and no warnings there is nothing to prepare")
  void nothingToPrepare() {
    when(bettingTableService.getShapedPitchesForBetting(5L)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdOrderByStartDateAsc(5L)).thenReturn(List.of());

    AgentLoopReport r = builder.build(project, TODAY);

    assertThat(r.hasFindings()).isFalse();
    assertThat(r.facts()).contains("No upcoming cycle is planned yet.").contains("No shaped pitches are waiting");
  }

  @Test
  @DisplayName("If capacity can't be computed the brief still goes out with the pitches")
  void capacityFailureIsNotFatal() {
    when(bettingTableService.getShapedPitchesForBetting(5L)).thenReturn(List.of(pitch("A", 5)));
    when(cycleRepository.findByProjectIdOrderByStartDateAsc(5L))
        .thenReturn(List.of(cycle(2L, "Cycle 7", TODAY.plusDays(1), TODAY.plusDays(43))));
    when(bettingTableService.getCapacityAnalysis(2L)).thenThrow(new RuntimeException("no teams"));

    AgentLoopReport r = builder.build(project, TODAY);

    assertThat(r.hasFindings()).isTrue();
    assertThat(r.facts()).doesNotContain("Capacity:").contains("A — 5 days appetite");
  }
}
