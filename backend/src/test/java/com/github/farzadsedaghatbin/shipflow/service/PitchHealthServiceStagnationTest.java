package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.AlertSeverity;
import com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PitchHealthServiceStagnationTest {

  @Mock private PitchRepository pitchRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private WorkLogRepository workLogRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private BugReportRepository bugReportRepository;
  @Mock private HillChartPointRepository hillChartPointRepository;

  @InjectMocks private PitchHealthService pitchHealthService;

  private Cycle testCycle;
  private Pitch testPitch;

  @BeforeEach
  void setUp() {
    testCycle = Cycle.builder()
        .id(1L)
        .name("Test Cycle")
        .startDate(LocalDate.now().minusDays(30))
        .endDate(LocalDate.now().plusDays(12))
        .phase(CyclePhase.SHAPING_BUILDING)
        .build();

    testPitch = Pitch.builder()
        .id(1L)
        .title("Test Pitch")
        .status(PitchStatus.IN_PROGRESS)
        .createdAt(LocalDateTime.now().minusDays(20))
        .updatedAt(LocalDateTime.now().minusDays(10))
        .cycle(testCycle)
        .build();
  }

  @Nested
  @DisplayName("detectStagnatingPitches")
  class DetectStagnatingPitchesTests {

    @Test
    @DisplayName("should detect hill chart stalled stagnation")
    void shouldDetectHillChartStalledStagnation() {
      HillChartPoint stagnantScope = HillChartPoint.builder()
          .id(1L)
          .pitch(testPitch)
          .scope("User Login")
          .position(25) // Under uphill max threshold (30)
          .updatedAt(LocalDateTime.now().minusDays(10)) // Older than scopeStagnationDays (7)
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));
      when(hillChartPointRepository.findByPitchId(1L)).thenReturn(List.of(stagnantScope));
      when(workLogRepository.findByPitchId(1L)).thenReturn(Collections.emptyList());

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).hasSize(1);
      assertThat(alerts.get(0).getStagnationType()).isEqualTo(StagnationType.HILL_CHART_STALLED);
      assertThat(alerts.get(0).getStagnantScopes()).contains("User Login");
      assertThat(alerts.get(0).getRecommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("should detect stuck at peak stagnation")
    void shouldDetectStuckAtPeakStagnation() {
      HillChartPoint peakStuckScope = HillChartPoint.builder()
          .id(1L)
          .pitch(testPitch)
          .scope("Database Design")
          .position(50) // At the peak (between scopePeakMin=45 and scopePeakMax=55)
          .updatedAt(LocalDateTime.now().minusDays(7)) // Older than peakStuckDays (5)
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));
      when(hillChartPointRepository.findByPitchId(1L)).thenReturn(List.of(peakStuckScope));
      when(workLogRepository.findByPitchId(1L)).thenReturn(Collections.emptyList());

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).hasSize(1);
      assertThat(alerts.get(0).getStagnationType()).isEqualTo(StagnationType.STUCK_AT_PEAK);
      assertThat(alerts.get(0).getDescription()).contains("decision point");
    }

    @Test
    @DisplayName("should detect compound stagnation")
    void shouldDetectCompoundStagnation() {
      HillChartPoint stagnantScope = HillChartPoint.builder()
          .id(1L)
          .pitch(testPitch)
          .scope("User Login")
          .position(25) // Under uphill max (30), stagnant
          .updatedAt(LocalDateTime.now().minusDays(10)) // Older than scopeStagnationDays (7)
          .build();

      HillChartPoint peakStuckScope = HillChartPoint.builder()
          .id(2L)
          .pitch(testPitch)
          .scope("Database Design")
          .position(50) // At peak (45-55)
          .updatedAt(LocalDateTime.now().minusDays(7)) // Older than peakStuckDays (5)
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));
      when(hillChartPointRepository.findByPitchId(1L)).thenReturn(List.of(stagnantScope, peakStuckScope));
      when(workLogRepository.findByPitchId(1L)).thenReturn(Collections.emptyList());

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).hasSize(1);
      assertThat(alerts.get(0).getStagnationType()).isEqualTo(StagnationType.COMPOUND_STAGNATION);
    }

    @Test
    @DisplayName("should skip completed pitches")
    void shouldSkipCompletedPitches() {
      testPitch.setStatus(PitchStatus.DONE);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).isEmpty();
      verify(hillChartPointRepository, never()).findByPitchId(anyLong());
    }

    @Test
    @DisplayName("should skip cancelled pitches")
    void shouldSkipCancelledPitches() {
      testPitch.setStatus(PitchStatus.CANCELLED);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for healthy pitches")
    void shouldReturnEmptyListForHealthyPitches() {
      HillChartPoint healthyScope = HillChartPoint.builder()
          .id(1L)
          .pitch(testPitch)
          .scope("Active Feature")
          .position(70) // Downhill
          .updatedAt(LocalDateTime.now().minusDays(1)) // Recent activity
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch));
      when(hillChartPointRepository.findByPitchId(1L)).thenReturn(List.of(healthyScope));
      when(workLogRepository.findByPitchId(1L)).thenReturn(Collections.emptyList());

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("should sort alerts by severity then days since activity")
    void shouldSortAlertsBySeverityThenDays() {
      Pitch pitch2 = Pitch.builder()
          .id(2L)
          .title("Pitch 2")
          .status(PitchStatus.IN_PROGRESS)
          .createdAt(LocalDateTime.now().minusDays(20))
          .updatedAt(LocalDateTime.now().minusDays(20)) // Older
          .cycle(testCycle)
          .build();

      HillChartPoint scope1 = HillChartPoint.builder()
          .id(1L)
          .pitch(testPitch)
          .scope("Scope 1")
          .position(30)
          .updatedAt(LocalDateTime.now().minusDays(10))
          .build();

      HillChartPoint scope2 = HillChartPoint.builder()
          .id(2L)
          .pitch(pitch2)
          .scope("Scope 2")
          .position(30)
          .updatedAt(LocalDateTime.now().minusDays(20)) // More stagnant
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(testPitch, pitch2));
      when(hillChartPointRepository.findByPitchId(1L)).thenReturn(List.of(scope1));
      when(hillChartPointRepository.findByPitchId(2L)).thenReturn(List.of(scope2));
      when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

      List<StagnationAlertDTO> alerts = pitchHealthService.detectStagnatingPitches(1L);

      assertThat(alerts).hasSize(2);
      // Higher severity or more days should come first
      assertThat(alerts.get(0).getDaysSinceActivity()).isGreaterThanOrEqualTo(alerts.get(1).getDaysSinceActivity());
    }
  }

  @Nested
  @DisplayName("AlertSeverity calculation")
  class AlertSeverityTests {

    @Test
    @DisplayName("should return CRITICAL for late cycle with low progress")
    void shouldReturnCriticalForLateCycleLowProgress() {
      AlertSeverity severity = StagnationAlertDTO.calculateSeverity(7, 30, 80);
      assertThat(severity).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("should return WARNING for moderate stagnation")
    void shouldReturnWarningForModerateStagnation() {
      AlertSeverity severity = StagnationAlertDTO.calculateSeverity(8, 50, 40);
      assertThat(severity).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("should return INFO for minor stagnation")
    void shouldReturnInfoForMinorStagnation() {
      AlertSeverity severity = StagnationAlertDTO.calculateSeverity(3, 70, 30);
      assertThat(severity).isEqualTo(AlertSeverity.INFO);
    }
  }
}
