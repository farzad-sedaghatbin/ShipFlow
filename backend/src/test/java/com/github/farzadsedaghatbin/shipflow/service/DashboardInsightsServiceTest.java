package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardInsightDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardInsightsResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardInsightsServiceTest {

  @Mock private CycleRepository cycleRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private ChatLanguageModel chatLanguageModel;

  private DashboardInsightsService serviceWithLlm;
  private DashboardInsightsService serviceWithoutLlm;

  @BeforeEach
  void setUp() {
    serviceWithLlm = new DashboardInsightsService(cycleRepository, pitchRepository, chatLanguageModel, null);
    serviceWithoutLlm = new DashboardInsightsService(cycleRepository, pitchRepository, null, null);
  }

  // ── Helper builders ──────────────────────────────────────────────────────

  private Cycle activeCycle(Long id, LocalDate start, LocalDate end) {
    return Cycle.builder()
        .id(id)
        .name("Cycle " + id)
        .startDate(start)
        .endDate(end)
        .isActive(true)
        .phase(CyclePhase.SHAPING_BUILDING)
        .build();
  }

  private Cycle inactiveCycle(Long id, LocalDate start, LocalDate end) {
    return Cycle.builder()
        .id(id)
        .name("Cycle " + id)
        .startDate(start)
        .endDate(end)
        .isActive(false)
        .phase(CyclePhase.BETTING_COOLDOWN)
        .build();
  }

  private Pitch pitch(Long id, PitchStatus status, LocalDateTime createdAt) {
    return Pitch.builder()
        .id(id)
        .title("Pitch " + id)
        .status(status)
        .createdAt(createdAt)
        .updatedAt(createdAt)
        .build();
  }

  // ── No insights when no cycles ───────────────────────────────────────────

  @Test
  void getInsights_noCycles_returnsEmptyList() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of());

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.getInsights()).isEmpty();
    assertThat(result.getAiSummary()).isNull();
    assertThat(result.isAiEnabled()).isFalse();
    assertThat(result.getComputedAt()).isNotNull();
  }

  // ── Overdue pitches detection ─────────────────────────────────────────────

  @Test
  void getInsights_overduePitches_createsHighSeverityInsight() {
    LocalDate past = LocalDate.now().minusDays(5);
    Cycle overdueCycle = inactiveCycle(10L, past.minusDays(30), past);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(overdueCycle));

    Pitch p1 = pitch(1L, PitchStatus.IN_PROGRESS, past.minusDays(10).atStartOfDay());
    Pitch p2 = pitch(2L, PitchStatus.STARTED, past.minusDays(8).atStartOfDay());
    Pitch p3 = pitch(3L, PitchStatus.DONE, past.minusDays(2).atStartOfDay());

    when(pitchRepository.findByCycleIdNotDeleted(10L)).thenReturn(List.of(p1, p2, p3));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    List<DashboardInsightDTO> overdue =
        result.getInsights().stream()
            .filter(i -> "OVERDUE_PITCHES".equals(i.getType()))
            .toList();
    assertThat(overdue).hasSize(1);
    assertThat(overdue.get(0).getSeverity()).isEqualTo("HIGH");
    assertThat(overdue.get(0).getCount()).isEqualTo(2);
  }

  @Test
  void getInsights_allPitchesDone_noOverdueInsight() {
    LocalDate past = LocalDate.now().minusDays(3);
    Cycle overdueCycle = inactiveCycle(11L, past.minusDays(30), past);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(overdueCycle));
    when(pitchRepository.findByCycleIdNotDeleted(11L))
        .thenReturn(List.of(pitch(1L, PitchStatus.DONE, past.minusDays(5).atStartOfDay())));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.getInsights()).noneMatch(i -> "OVERDUE_PITCHES".equals(i.getType()));
  }

  // ── At-risk cycle detection ───────────────────────────────────────────────

  @Test
  void getInsights_cycleEndingSoon_withManyIncompletePitches_createsAtRiskInsight() {
    LocalDate end = LocalDate.now().plusDays(3);
    Cycle risky = activeCycle(20L, end.minusDays(30), end);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(risky));

    LocalDateTime created = end.minusDays(20).atStartOfDay();
    // 4 incomplete out of 5 → 80% → at risk
    when(pitchRepository.findByCycleIdNotDeleted(20L))
        .thenReturn(
            List.of(
                pitch(1L, PitchStatus.IN_PROGRESS, created),
                pitch(2L, PitchStatus.STARTED, created),
                pitch(3L, PitchStatus.PENDING, created),
                pitch(4L, PitchStatus.TESTING, created),
                pitch(5L, PitchStatus.DONE, created)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    List<DashboardInsightDTO> atRisk =
        result.getInsights().stream().filter(i -> "AT_RISK_CYCLE".equals(i.getType())).toList();
    assertThat(atRisk).hasSize(1);
    assertThat(atRisk.get(0).getSeverity()).isEqualTo("MEDIUM");
    assertThat(atRisk.get(0).getTargetRoute()).isEqualTo("/cycles/20");
    assertThat(atRisk.get(0).getCount()).isEqualTo(4);
  }

  @Test
  void getInsights_cycleEndingSoon_mostPitchesDone_noAtRiskInsight() {
    LocalDate end = LocalDate.now().plusDays(2);
    Cycle cycle = activeCycle(21L, end.minusDays(30), end);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(cycle));

    LocalDateTime created = end.minusDays(20).atStartOfDay();
    // Only 1 incomplete out of 4 → 25% → safe
    when(pitchRepository.findByCycleIdNotDeleted(21L))
        .thenReturn(
            List.of(
                pitch(1L, PitchStatus.DONE, created),
                pitch(2L, PitchStatus.DONE, created),
                pitch(3L, PitchStatus.DONE, created),
                pitch(4L, PitchStatus.IN_PROGRESS, created)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.getInsights()).noneMatch(i -> "AT_RISK_CYCLE".equals(i.getType()));
  }

  // ── Scope creep detection ─────────────────────────────────────────────────

  @Test
  void getInsights_pitchAddedLate_createsScopeCreepInsight() {
    LocalDate start = LocalDate.now().minusDays(20);
    LocalDate end = LocalDate.now().plusDays(10);
    Cycle cycle = activeCycle(30L, start, end);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(cycle));

    // Pitch created 10 days after start — clearly late
    LocalDateTime lateCreation = start.plusDays(10).atStartOfDay();
    LocalDateTime onTime = start.plusDays(1).atStartOfDay();

    when(pitchRepository.findByCycleIdNotDeleted(30L))
        .thenReturn(
            List.of(
                pitch(1L, PitchStatus.IN_PROGRESS, onTime),
                pitch(2L, PitchStatus.IN_PROGRESS, lateCreation)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    List<DashboardInsightDTO> scopeCreep =
        result.getInsights().stream().filter(i -> "SCOPE_CREEP".equals(i.getType())).toList();
    assertThat(scopeCreep).hasSize(1);
    assertThat(scopeCreep.get(0).getSeverity()).isEqualTo("MEDIUM");
    assertThat(scopeCreep.get(0).getCount()).isEqualTo(1);
  }

  @Test
  void getInsights_allPitchesAddedOnTime_noScopeCreepInsight() {
    LocalDate start = LocalDate.now().minusDays(20);
    LocalDate end = LocalDate.now().plusDays(10);
    Cycle cycle = activeCycle(31L, start, end);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(cycle));

    LocalDateTime onTime = start.plusDays(2).atStartOfDay();
    when(pitchRepository.findByCycleIdNotDeleted(31L))
        .thenReturn(List.of(pitch(1L, PitchStatus.IN_PROGRESS, onTime)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.getInsights()).noneMatch(i -> "SCOPE_CREEP".equals(i.getType()));
  }

  // ── Velocity trend ────────────────────────────────────────────────────────

  @Test
  void getInsights_decliningVelocity_createsLowSeverityInsight() {
    LocalDate base = LocalDate.now().minusDays(90);
    // newest first (matches findByProjectIdOrderByStartDateDesc)
    Cycle c3 = inactiveCycle(40L, base.plusDays(60), base.plusDays(90)); // latest: 1 done
    Cycle c2 = inactiveCycle(41L, base.plusDays(30), base.plusDays(60)); // prev1: 5 done
    Cycle c1 = inactiveCycle(42L, base, base.plusDays(30));              // prev2: 5 done

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(c3, c2, c1));

    LocalDateTime t = base.atStartOfDay();
    when(pitchRepository.findByCycleIdNotDeleted(40L))
        .thenReturn(List.of(pitch(1L, PitchStatus.DONE, t)));
    when(pitchRepository.findByCycleIdNotDeleted(41L))
        .thenReturn(
            List.of(
                pitch(2L, PitchStatus.DONE, t),
                pitch(3L, PitchStatus.DONE, t),
                pitch(4L, PitchStatus.DONE, t),
                pitch(5L, PitchStatus.DONE, t),
                pitch(6L, PitchStatus.DONE, t)));
    when(pitchRepository.findByCycleIdNotDeleted(42L))
        .thenReturn(
            List.of(
                pitch(7L, PitchStatus.DONE, t),
                pitch(8L, PitchStatus.DONE, t),
                pitch(9L, PitchStatus.DONE, t),
                pitch(10L, PitchStatus.DONE, t),
                pitch(11L, PitchStatus.DONE, t)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    List<DashboardInsightDTO> velocity =
        result.getInsights().stream().filter(i -> "VELOCITY_TREND".equals(i.getType())).toList();
    assertThat(velocity).hasSize(1);
    assertThat(velocity.get(0).getSeverity()).isEqualTo("LOW");
    assertThat(velocity.get(0).getTitle()).contains("Declining");
  }

  @Test
  void getInsights_improvingVelocity_createsInfoInsight() {
    LocalDate base = LocalDate.now().minusDays(90);
    Cycle c3 = inactiveCycle(50L, base.plusDays(60), base.plusDays(90)); // latest: 10 done
    Cycle c2 = inactiveCycle(51L, base.plusDays(30), base.plusDays(60)); // prev1: 4 done
    Cycle c1 = inactiveCycle(52L, base, base.plusDays(30));              // prev2: 4 done

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(c3, c2, c1));

    LocalDateTime t = base.atStartOfDay();
    // 10 done in latest
    when(pitchRepository.findByCycleIdNotDeleted(50L))
        .thenReturn(
            List.of(
                pitch(1L, PitchStatus.DONE, t), pitch(2L, PitchStatus.DONE, t),
                pitch(3L, PitchStatus.DONE, t), pitch(4L, PitchStatus.DONE, t),
                pitch(5L, PitchStatus.DONE, t), pitch(6L, PitchStatus.DONE, t),
                pitch(7L, PitchStatus.DONE, t), pitch(8L, PitchStatus.DONE, t),
                pitch(9L, PitchStatus.DONE, t), pitch(10L, PitchStatus.DONE, t)));
    // 4 done in each prior
    when(pitchRepository.findByCycleIdNotDeleted(51L))
        .thenReturn(
            List.of(
                pitch(11L, PitchStatus.DONE, t), pitch(12L, PitchStatus.DONE, t),
                pitch(13L, PitchStatus.DONE, t), pitch(14L, PitchStatus.DONE, t)));
    when(pitchRepository.findByCycleIdNotDeleted(52L))
        .thenReturn(
            List.of(
                pitch(15L, PitchStatus.DONE, t), pitch(16L, PitchStatus.DONE, t),
                pitch(17L, PitchStatus.DONE, t), pitch(18L, PitchStatus.DONE, t)));

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    List<DashboardInsightDTO> velocity =
        result.getInsights().stream().filter(i -> "VELOCITY_TREND".equals(i.getType())).toList();
    assertThat(velocity).hasSize(1);
    assertThat(velocity.get(0).getSeverity()).isEqualTo("INFO");
    assertThat(velocity.get(0).getTitle()).contains("Improving");
  }

  @Test
  void getInsights_fewerThanThreeCompletedCycles_noVelocityInsight() {
    LocalDate base = LocalDate.now().minusDays(30);
    Cycle c1 = inactiveCycle(60L, base, base.plusDays(30));

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(c1));
    when(pitchRepository.findByCycleIdNotDeleted(60L)).thenReturn(List.of());

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.getInsights()).noneMatch(i -> "VELOCITY_TREND".equals(i.getType()));
  }

  // ── AI summary ────────────────────────────────────────────────────────────

  @Test
  void getInsights_withLlmAndInsights_populatesAiSummary() {
    LocalDate past = LocalDate.now().minusDays(5);
    Cycle overdueCycle = inactiveCycle(70L, past.minusDays(30), past);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(overdueCycle));
    when(pitchRepository.findByCycleIdNotDeleted(70L))
        .thenReturn(List.of(pitch(1L, PitchStatus.IN_PROGRESS, past.minusDays(10).atStartOfDay())));
    when(chatLanguageModel.generate(any(String.class))).thenReturn("One pitch is overdue.");

    DashboardInsightsResponseDTO result = serviceWithLlm.getInsights(1L);

    assertThat(result.getAiSummary()).isEqualTo("One pitch is overdue.");
    assertThat(result.isAiEnabled()).isTrue();
  }

  @Test
  void getInsights_withLlmButNoInsights_noAiSummaryCall() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of());

    DashboardInsightsResponseDTO result = serviceWithLlm.getInsights(1L);

    assertThat(result.getAiSummary()).isNull();
    verifyNoInteractions(chatLanguageModel);
  }

  @Test
  void getInsights_withLlmAndLlmFails_summaryIsNullButInsightsStillReturned() {
    LocalDate past = LocalDate.now().minusDays(5);
    Cycle overdueCycle = inactiveCycle(80L, past.minusDays(30), past);

    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of(overdueCycle));
    when(pitchRepository.findByCycleIdNotDeleted(80L))
        .thenReturn(List.of(pitch(1L, PitchStatus.IN_PROGRESS, past.minusDays(10).atStartOfDay())));
    when(chatLanguageModel.generate(any(String.class)))
        .thenThrow(new RuntimeException("LLM timeout"));

    DashboardInsightsResponseDTO result = serviceWithLlm.getInsights(1L);

    assertThat(result.getAiSummary()).isNull();
    assertThat(result.getInsights()).anyMatch(i -> "OVERDUE_PITCHES".equals(i.getType()));
  }

  // ── aiEnabled flag ────────────────────────────────────────────────────────

  @Test
  void getInsights_withoutLlm_aiEnabledIsFalse() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of());

    DashboardInsightsResponseDTO result = serviceWithoutLlm.getInsights(1L);

    assertThat(result.isAiEnabled()).isFalse();
  }

  @Test
  void getInsights_withLlm_aiEnabledIsTrue() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(List.of());

    DashboardInsightsResponseDTO result = serviceWithLlm.getInsights(1L);

    assertThat(result.isAiEnabled()).isTrue();
  }
}
