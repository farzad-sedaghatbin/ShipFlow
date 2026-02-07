package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.signal.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.math.BigDecimal;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for CycleSignalService - v0.5 "Insight, Not Metrics" feature.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CycleSignalService Tests")
class CycleSignalServiceTest {

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private RetroItemRepository retroItemRepository;

  @Mock
  private RetroRepository retroRepository;

  @Mock
  private PitchRiskHistoryRepository riskHistoryRepository;

  @Mock
  private CycleSignalCacheRepository signalCacheRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private AIConfig aiConfig;

  @Mock
  private ChatLanguageModel chatLanguageModel;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private CycleSignalService cycleSignalService;

  private Project project;
  private Cycle cycle1, cycle2, cycle3;
  private List<Pitch> pitches;

  @BeforeEach
  void setUp() {
    project = Project.builder()
        .id(1L)
        .name("Test Project")
        .projectKey("TEST")
        .build();

    cycle1 = Cycle.builder()
        .id(1L)
        .name("Cycle 1")
        .project(project)
        .startDate(LocalDate.of(2026, 1, 1))
        .endDate(LocalDate.of(2026, 2, 12))
        .build();

    cycle2 = Cycle.builder()
        .id(2L)
        .name("Cycle 2")
        .project(project)
        .startDate(LocalDate.of(2026, 2, 13))
        .endDate(LocalDate.of(2026, 3, 26))
        .build();

    cycle3 = Cycle.builder()
        .id(3L)
        .name("Cycle 3")
        .project(project)
        .startDate(LocalDate.of(2026, 3, 27))
        .endDate(LocalDate.of(2026, 5, 8))
        .build();

    pitches = createSamplePitches();
  }

  private List<Pitch> createSamplePitches() {
    List<Pitch> pitches = new ArrayList<>();
    
    // Pitch 1: Done, under budget
    Pitch p1 = Pitch.builder()
        .id(1L)
        .title("Feature A")
        .appetiteDays(5)
        .status(PitchStatus.DONE)
        .cycle(cycle1)
        .build();
    pitches.add(p1);

    // Pitch 2: Done, over budget
    Pitch p2 = Pitch.builder()
        .id(2L)
        .title("Feature B")
        .appetiteDays(3)
        .status(PitchStatus.DONE)
        .cycle(cycle1)
        .build();
    pitches.add(p2);

    // Pitch 3: In progress
    Pitch p3 = Pitch.builder()
        .id(3L)
        .title("Feature C")
        .appetiteDays(4)
        .status(PitchStatus.IN_PROGRESS)
        .cycle(cycle1)
        .build();
    pitches.add(p3);

    return pitches;
  }

  @Nested
  @DisplayName("getProjectSignals Tests")
  class GetProjectSignalsTests {

    @Test
    @DisplayName("Should return signals for project with valid data")
    void shouldReturnSignalsForProject() {
      // Given
      when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
      when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
          .thenReturn(List.of(cycle3, cycle2, cycle1));
      when(pitchRepository.findByCycleIdNotDeleted(anyLong()))
          .thenReturn(pitches);
      when(workLogRepository.findByPitchId(anyLong()))
          .thenReturn(List.of(
              createWorkLog(1L, 30.0), // pitch 1: 30h for 5 days (75%)
              createWorkLog(2L, 30.0)  // pitch 2: 30h for 3 days (125%)
          ));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(anyLong()))
          .thenReturn(List.of());
      when(riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(anyLong()))
          .thenReturn(List.of());

      // When
      CycleSignalsDTO result = cycleSignalService.getProjectSignals(1L, 6);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getProjectId()).isEqualTo(1L);
      assertThat(result.getOverallHealthScore()).isBetween(0, 100);
    }
  }

  @Nested
  @DisplayName("getCycleSignals Tests")
  class GetCycleSignalsTests {

    @Test
    @DisplayName("Should return signals for specific cycle")
    void shouldReturnSignalsForCycle() {
      // Given
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle1));
      when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
          .thenReturn(List.of(cycle1));
      when(pitchRepository.findByCycleIdNotDeleted(1L))
          .thenReturn(pitches);
      when(workLogRepository.findByPitchId(anyLong()))
          .thenReturn(List.of(createWorkLog(1L, 40.0)));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(anyLong()))
          .thenReturn(List.of());
      when(riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(anyLong()))
          .thenReturn(List.of());

      // When
      CycleSignalsDTO result = cycleSignalService.getCycleSignals(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getCycleId()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("Appetite Accuracy Signal Tests")
  class AppetiteAccuracyTests {

    @Test
    @DisplayName("Should detect improving trend when accuracy gets better over cycles")
    void shouldDetectImprovingTrend() {
      // Given: Cycles with improving accuracy ratio
      // Cycle 1: 120% (over budget)
      // Cycle 2: 100% (on budget)
      // Cycle 3: 90% (under budget)
      
      when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
      when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
          .thenReturn(List.of(cycle3, cycle2, cycle1));
      
      // Setup pitches for each cycle with decreasing actual/appetite ratio
      Pitch p1 = Pitch.builder().id(1L).appetiteDays(5).status(PitchStatus.DONE).cycle(cycle1).build();
      Pitch p2 = Pitch.builder().id(2L).appetiteDays(5).status(PitchStatus.DONE).cycle(cycle2).build();
      Pitch p3 = Pitch.builder().id(3L).appetiteDays(5).status(PitchStatus.DONE).cycle(cycle3).build();
      
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(List.of(p1));
      when(pitchRepository.findByCycleIdNotDeleted(2L)).thenReturn(List.of(p2));
      when(pitchRepository.findByCycleIdNotDeleted(3L)).thenReturn(List.of(p3));
      
      // Cycle 1: 48h for 5 days (120%)
      when(workLogRepository.findByPitchId(1L)).thenReturn(List.of(createWorkLog(1L, 48.0)));
      // Cycle 2: 40h for 5 days (100%)
      when(workLogRepository.findByPitchId(2L)).thenReturn(List.of(createWorkLog(2L, 40.0)));
      // Cycle 3: 36h for 5 days (90%)
      when(workLogRepository.findByPitchId(3L)).thenReturn(List.of(createWorkLog(3L, 36.0)));
      
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(anyLong()))
          .thenReturn(List.of());
      when(riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(anyLong()))
          .thenReturn(List.of());

      // When
      CycleSignalsDTO result = cycleSignalService.getProjectSignals(1L, 6);

      // Then
      assertThat(result.getAppetiteAccuracy()).isNotNull();
      // With trend slope < 0 (improving = closer to 1.0 or under)
      assertThat(result.getAppetiteAccuracy().getTrend())
          .isIn(AppetiteAccuracySignalDTO.TrendDirection.IMPROVING, 
                AppetiteAccuracySignalDTO.TrendDirection.STABLE);
    }
  }

  @Nested
  @DisplayName("Retro Follow-Through Signal Tests")
  class RetroFollowThroughTests {

    @Test
    @DisplayName("Should calculate follow-through rate correctly")
    void shouldCalculateFollowThroughRate() {
      // Given
      Retrospective retro = Retrospective.builder()
          .id(1L)
          .title("Sprint 1 Retro")
          .cycle(cycle1)
          .build();
      
      RetroItem item1 = RetroItem.builder()
          .id(1L)
          .content("Improve code reviews")
          .columnType(RetroColumnType.ACTIONS)
          .actedOn(true)
          .createdAt(LocalDateTime.now().minusDays(10))
          .build();
      
      RetroItem item2 = RetroItem.builder()
          .id(2L)
          .content("Better documentation")
          .columnType(RetroColumnType.ACTIONS)
          .actedOn(false)
          .createdAt(LocalDateTime.now().minusDays(10))
          .build();
      
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle1));
      when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
          .thenReturn(List.of(cycle1));
      when(pitchRepository.findByCycleIdNotDeleted(anyLong()))
          .thenReturn(List.of());
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(1L))
          .thenReturn(List.of(retro));
      when(retroItemRepository.findByRetrospectiveIdAndColumnType(1L, RetroColumnType.ACTIONS))
          .thenReturn(List.of(item1, item2));
      when(retroItemRepository.countActedOnByRetrospectiveId(1L))
          .thenReturn(1L);
      when(riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(anyLong()))
          .thenReturn(List.of());

      // When
      CycleSignalsDTO result = cycleSignalService.getCycleSignals(1L);

      // Then
      assertThat(result.getRetroFollowThrough()).isNotNull();
      assertThat(result.getRetroFollowThrough().getTotalActionItems()).isEqualTo(2);
      assertThat(result.getRetroFollowThrough().getActedOnCount()).isEqualTo(1);
      assertThat(result.getRetroFollowThrough().getFollowThroughRatePercent()).isEqualTo(50.0);
    }
  }

  @Nested
  @DisplayName("Health Score Calculation Tests")
  class HealthScoreTests {

    @Test
    @DisplayName("Should calculate health score between 0 and 100")
    void shouldCalculateValidHealthScore() {
      // Given
      when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
      when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
          .thenReturn(List.of(cycle1));
      when(pitchRepository.findByCycleIdNotDeleted(1L))
          .thenReturn(pitches);
      when(workLogRepository.findByPitchId(anyLong()))
          .thenReturn(List.of(createWorkLog(1L, 40.0)));
      when(retroRepository.findByCycleIdOrderByCreatedAtDesc(anyLong()))
          .thenReturn(List.of());
      when(riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(anyLong()))
          .thenReturn(List.of());

      // When
      CycleSignalsDTO result = cycleSignalService.getProjectSignals(1L, 6);

      // Then
      assertThat(result.getOverallHealthScore()).isBetween(0, 100);
    }
  }

  private WorkLog createWorkLog(Long id, double hours) {
    return WorkLog.builder()
        .id(id)
        .hoursSpent(BigDecimal.valueOf(hours))
        .date(LocalDate.now())
        .build();
  }
}
