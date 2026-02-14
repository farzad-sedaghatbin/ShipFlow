package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.report.EnhancedCycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.RiskDistributionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.signal.CycleSignalsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.narrative.CycleSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import com.github.farzadsedaghatbin.shipflow.repository.*;
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
 * Comprehensive tests for ReportService with focus on enhanced reporting
 * features.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Tests")
class ReportServiceTest {

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private TeamAssignmentRepository teamAssignmentRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private RiskAnalysisService riskAnalysisService;

  @Mock
  private CycleSignalService cycleSignalService;

  @Mock
  private CycleNarrativeService cycleNarrativeService;

  @Mock
  private CapacityConfigService capacityConfigService;

  @InjectMocks
  private ReportService reportService;

  private Cycle cycle;
  private Project project;
  private Team team;
  private Person person1, person2;
  private Pitch pitch1, pitch2, pitch3;
  private List<WorkLog> workLogs;

  @BeforeEach
  void setUp() {
    // Setup project
    project = Project.builder().id(1L).name("Test Project").projectKey("TEST").build();

    // Setup cycle
    cycle = Cycle.builder().id(1L).name("Q1 2026").startDate(LocalDate.of(2026, 1, 1))
        .endDate(LocalDate.of(2026, 2, 12)).project(project).build();

    // Setup team
    team = Team.builder().id(1L).name("Alpha Team").build();

    // Setup persons
    person1 = Person.builder().id(1L).name("Alice Johnson").build();

    person2 = Person.builder().id(2L).name("Bob Smith").build();

    // Setup pitches with different statuses
    pitch1 = Pitch.builder().id(1L).title("User Dashboard").appetiteDays(5).cycle(cycle).team(team)
        .status(PitchStatus.DONE).build();

    pitch2 = Pitch.builder().id(2L).title("API Integration").appetiteDays(3).cycle(cycle).team(team)
        .status(PitchStatus.IN_PROGRESS).build();

    pitch3 = Pitch.builder().id(3L).title("Mobile App").appetiteDays(4).cycle(cycle).team(team)
        .status(PitchStatus.PENDING).build();

    // Setup work logs
    workLogs = new ArrayList<>();
    // pitch1: 35 hours (under budget: 40 hours)
    workLogs.add(createWorkLog(1L, pitch1, person1, 20.0, LocalDate.of(2026, 1, 5)));
    workLogs.add(createWorkLog(2L, pitch1, person2, 15.0, LocalDate.of(2026, 1, 6)));

    // pitch2: 30 hours (over budget: 24 hours)
    workLogs.add(createWorkLog(3L, pitch2, person1, 18.0, LocalDate.of(2026, 1, 10)));
    workLogs.add(createWorkLog(4L, pitch2, person2, 12.0, LocalDate.of(2026, 1, 11)));

    // pitch3: 0 hours (not started)

    // Mock v0.5 services with empty/default responses
    when(cycleSignalService.getCycleSignals(anyLong())).thenReturn(CycleSignalsDTO.builder()
        .projectId(1L)
        .cycleId(1L)
        .cyclesAnalyzed(1)
        .overallHealthScore(75)
        .analyzedAt(LocalDateTime.now())
        .build());
    
    when(cycleNarrativeService.getCycleSummary(anyLong())).thenReturn(CycleSummaryDTO.builder()
        .cycleId(1L)
        .cycleName("Q1 2026")
        .projectName("Test Project")
        .build());

    // Setup capacity config mock - dynamically calculate hours based on appetite
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any(Pitch.class)))
        .thenAnswer(invocation -> {
          Pitch pitch = invocation.getArgument(0);
          return pitch.getAppetiteDays() * 8.0; // 8 hours/day default
        });
    lenient().when(capacityConfigService.getOrganizationDefaultHoursPerDay()).thenReturn(8.0);
  }

  private WorkLog createWorkLog(Long id, Pitch pitch, Person person, Double hours, LocalDate date) {
    return WorkLog.builder().id(id).pitch(pitch).person(person).hoursSpent(BigDecimal.valueOf(hours)).date(date)
        .note("Work on " + pitch.getTitle()).build();
  }

  @Nested
  @DisplayName("Enhanced Cycle Report Tests")
  @MockitoSettings(strictness = Strictness.LENIENT)
  class EnhancedCycleReportTests {

    @Test
    @DisplayName("Should generate comprehensive enhanced report with all metrics")
    void shouldGenerateEnhancedReport() {
      // Given
      List<Pitch> pitches = Arrays.asList(pitch1, pitch2, pitch3);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(workLogs);

      // Mock person repository
      when(personRepository.findById(1L)).thenReturn(Optional.of(person1));
      when(personRepository.findById(2L)).thenReturn(Optional.of(person2));

      // Mock team assignments
      TeamAssignment assignment1 = TeamAssignment.builder().person(person1).team(team)
          .role(TeamMemberRole.FULLSTACK).build();
      when(teamAssignmentRepository.findByPersonAndCycle(1L, 1L))
          .thenReturn(Collections.singletonList(assignment1));
      when(teamAssignmentRepository.findByPersonAndCycle(2L, 1L)).thenReturn(Collections.emptyList());

      // Mock task statistics
      when(taskRepository.countByCycleId(1L)).thenReturn(10);
      when(taskRepository.countByCycleIdAndStatus(1L, TaskStatus.DONE)).thenReturn(7);
      when(taskRepository.getTotalEstimateHoursByCycleId(1L)).thenReturn(50.0);
      when(taskRepository.getTotalActualHoursByCycleId(1L)).thenReturn(45.0);

      // Mock risk analysis
      PitchRiskDTO lowRisk = createPitchRisk(1L, RiskLevel.LOW, 20);
      PitchRiskDTO highRisk = createPitchRisk(2L, RiskLevel.HIGH, 65);
      PitchRiskDTO mediumRisk = createPitchRisk(3L, RiskLevel.MEDIUM, 40);

      when(riskAnalysisService.analyzePitchRisk(pitch1, false)).thenReturn(lowRisk);
      when(riskAnalysisService.analyzePitchRisk(pitch2, false)).thenReturn(highRisk);
      when(riskAnalysisService.analyzePitchRisk(pitch3, false)).thenReturn(mediumRisk);

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      assertThat(report).isNotNull();
      assertThat(report.getCycleId()).isEqualTo(1L);
      assertThat(report.getCycleName()).isEqualTo("Q1 2026");
      assertThat(report.getProjectName()).isEqualTo("Test Project");

      // Verify pitch metrics
      assertThat(report.getTotalPitches()).isEqualTo(3);
      assertThat(report.getCompletedPitches()).isEqualTo(1);
      assertThat(report.getInProgressPitches()).isEqualTo(1);
      assertThat(report.getNotStartedPitches()).isEqualTo(1);

      // Verify hours and efficiency (pitch1: 40h appetite, 35h actual; pitch2: 24h,
      // 30h; pitch3:
      // 32h, 0h)
      assertThat(report.getTotalAppetiteHours()).isEqualTo(96.0); // (5+3+4) * 8
      assertThat(report.getTotalActualHours()).isEqualTo(65.0); // 35 + 30
      assertThat(report.getVarianceHours()).isEqualTo(-31.0); // 65 - 96
      assertThat(report.getVariancePercentage()).isCloseTo(-32.29, within(0.1));
      assertThat(report.getEfficiencyPercentage()).isCloseTo(67.71, within(0.1));

      // Verify task statistics
      assertThat(report.getTotalTasks()).isEqualTo(10);
      assertThat(report.getCompletedTasks()).isEqualTo(7);
      assertThat(report.getTotalTaskEstimateHours()).isEqualTo(50.0);
      assertThat(report.getTotalTaskActualHours()).isEqualTo(45.0);

      // Verify team member statistics
      assertThat(report.getTotalTeamMembers()).isEqualTo(2);
      assertThat(report.getAverageHoursPerMember()).isEqualTo(32.5); // 65 / 2
      assertThat(report.getMaxHoursPerMember()).isEqualTo(38.0); // person1
      assertThat(report.getMinHoursPerMember()).isEqualTo(27.0); // person2

      // Verify over-budget pitches
      assertThat(report.getOverBudgetPitches()).containsExactly("API Integration");

      // Verify risk distribution is calculated
      verify(riskAnalysisService, times(3)).analyzePitchRisk(any(Pitch.class), eq(false));
    }

    @Test
    @DisplayName("Should calculate correct risk distribution")
    void shouldCalculateRiskDistribution() {
      // Given
      List<Pitch> pitches = Arrays.asList(pitch1, pitch2, pitch3);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(workLogs);
      when(personRepository.findById(anyLong())).thenReturn(Optional.of(person1));
      when(teamAssignmentRepository.findByPersonAndCycle(anyLong(), anyLong()))
          .thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);

      // Mock risk analysis with specific levels
      when(riskAnalysisService.analyzePitchRisk(pitch1, false))
          .thenReturn(createPitchRisk(1L, RiskLevel.LOW, 15));
      when(riskAnalysisService.analyzePitchRisk(pitch2, false))
          .thenReturn(createPitchRisk(2L, RiskLevel.HIGH, 70));
      when(riskAnalysisService.analyzePitchRisk(pitch3, false))
          .thenReturn(createPitchRisk(3L, RiskLevel.MEDIUM, 45));

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      RiskDistributionDTO riskDist = report.getRiskDistribution();
      assertThat(riskDist).isNotNull();
      assertThat(riskDist.getLowRiskCount()).isEqualTo(1);
      assertThat(riskDist.getMediumRiskCount()).isEqualTo(1);
      assertThat(riskDist.getHighRiskCount()).isEqualTo(1);
      assertThat(riskDist.getCriticalRiskCount()).isEqualTo(0);
      assertThat(riskDist.getAverageRiskScore()).isCloseTo(43.33, within(0.1));
      assertThat(riskDist.getMaxRiskScore()).isEqualTo(70.0);
      assertThat(riskDist.getMinRiskScore()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should handle empty cycle with no pitches")
    void shouldHandleEmptyCycle() {
      // Given
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(Collections.emptyList());
      when(workLogRepository.findByCycleId(1L)).thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      assertThat(report).isNotNull();
      assertThat(report.getTotalPitches()).isEqualTo(0);
      assertThat(report.getTotalAppetiteHours()).isEqualTo(0.0);
      assertThat(report.getTotalActualHours()).isEqualTo(0.0);
      assertThat(report.getTotalTeamMembers()).isEqualTo(0);

      RiskDistributionDTO riskDist = report.getRiskDistribution();
      assertThat(riskDist.getLowRiskCount()).isEqualTo(0);
      assertThat(riskDist.getAverageRiskScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should identify top performers correctly")
    void shouldIdentifyTopPerformers() {
      // Given
      List<Pitch> pitches = Arrays.asList(pitch1, pitch2);

      // Create additional person with high efficiency
      Person person3 = Person.builder().id(3L).name("Charlie Brown").build();

      // Add work logs for person3 with high hours and good efficiency
      WorkLog log5 = createWorkLog(5L, pitch1, person3, 25.0, LocalDate.of(2026, 1, 7));
      WorkLog log6 = createWorkLog(6L, pitch1, person3, 24.0, LocalDate.of(2026, 1, 8));
      List<WorkLog> allLogs = new ArrayList<>(workLogs);
      allLogs.add(log5);
      allLogs.add(log6);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(allLogs);
      when(personRepository.findById(anyLong())).thenAnswer(inv -> {
        Long id = inv.getArgument(0);
        if (id == 1L)
          return Optional.of(person1);
        if (id == 2L)
          return Optional.of(person2);
        if (id == 3L)
          return Optional.of(person3);
        return Optional.empty();
      });
      when(teamAssignmentRepository.findByPersonAndCycle(anyLong(), anyLong()))
          .thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);
      when(riskAnalysisService.analyzePitchRisk(any(Pitch.class), anyBoolean()))
          .thenReturn(createPitchRisk(1L, RiskLevel.LOW, 20));

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then - Person3 has 49h over 2 days (24.5h/day) and should be top performer
      assertThat(report.getTopPerformers()).isNotEmpty();
      assertThat(report.getTopPerformers()).contains("Charlie Brown");
    }
  }

  @Nested
  @DisplayName("Risk Distribution Calculation Tests")
  class RiskDistributionTests {

    @Test
    @DisplayName("Should calculate risk distribution with all risk levels")
    void shouldCalculateAllRiskLevels() {
      // Given
      Pitch pitch4 = Pitch.builder().id(4L).title("Critical Feature").appetiteDays(2).cycle(cycle)
          .status(PitchStatus.IN_PROGRESS).build();

      List<Pitch> pitches = Arrays.asList(pitch1, pitch2, pitch3, pitch4);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(workLogs);
      when(personRepository.findById(anyLong())).thenReturn(Optional.of(person1));
      when(teamAssignmentRepository.findByPersonAndCycle(anyLong(), anyLong()))
          .thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);

      // Mock all risk levels
      when(riskAnalysisService.analyzePitchRisk(pitch1, false))
          .thenReturn(createPitchRisk(1L, RiskLevel.LOW, 10));
      when(riskAnalysisService.analyzePitchRisk(pitch2, false))
          .thenReturn(createPitchRisk(2L, RiskLevel.MEDIUM, 40));
      when(riskAnalysisService.analyzePitchRisk(pitch3, false))
          .thenReturn(createPitchRisk(3L, RiskLevel.HIGH, 70));
      when(riskAnalysisService.analyzePitchRisk(pitch4, false))
          .thenReturn(createPitchRisk(4L, RiskLevel.CRITICAL, 90));

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      RiskDistributionDTO riskDist = report.getRiskDistribution();
      assertThat(riskDist.getLowRiskCount()).isEqualTo(1);
      assertThat(riskDist.getMediumRiskCount()).isEqualTo(1);
      assertThat(riskDist.getHighRiskCount()).isEqualTo(1);
      assertThat(riskDist.getCriticalRiskCount()).isEqualTo(1);
      assertThat(riskDist.getAverageRiskScore()).isCloseTo(52.5, within(0.1));
      assertThat(riskDist.getMaxRiskScore()).isEqualTo(90.0);
      assertThat(riskDist.getMinRiskScore()).isEqualTo(10.0);
    }
  }

  @Nested
  @DisplayName("Variance Analysis Tests")
  class VarianceAnalysisTests {

    @Test
    @DisplayName("Should calculate positive variance for over-budget scenario")
    void shouldCalculatePositiveVariance() {
      // Given - pitch2 is over budget (30h actual vs 24h appetite)
      List<Pitch> pitches = Collections.singletonList(pitch2);
      List<WorkLog> pitch2Logs = workLogs.stream().filter(wl -> wl.getPitch().getId().equals(2L)).toList();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(pitch2Logs);
      when(personRepository.findById(anyLong())).thenReturn(Optional.of(person1));
      when(teamAssignmentRepository.findByPersonAndCycle(anyLong(), anyLong()))
          .thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);
      when(riskAnalysisService.analyzePitchRisk(any(Pitch.class), anyBoolean()))
          .thenReturn(createPitchRisk(2L, RiskLevel.HIGH, 65));

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      assertThat(report.getTotalAppetiteHours()).isEqualTo(24.0); // 3 days * 8h
      assertThat(report.getTotalActualHours()).isEqualTo(30.0);
      assertThat(report.getVarianceHours()).isEqualTo(6.0); // Over budget
      assertThat(report.getVariancePercentage()).isCloseTo(25.0, within(0.1));
      assertThat(report.getOverBudgetPitches()).containsExactly("API Integration");
    }

    @Test
    @DisplayName("Should calculate negative variance for under-budget scenario")
    void shouldCalculateNegativeVariance() {
      // Given - pitch1 is under budget (35h actual vs 40h appetite)
      List<Pitch> pitches = Collections.singletonList(pitch1);
      List<WorkLog> pitch1Logs = workLogs.stream().filter(wl -> wl.getPitch().getId().equals(1L)).toList();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));
      when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(pitches);
      when(workLogRepository.findByCycleId(1L)).thenReturn(pitch1Logs);
      when(personRepository.findById(anyLong())).thenReturn(Optional.of(person1));
      when(teamAssignmentRepository.findByPersonAndCycle(anyLong(), anyLong()))
          .thenReturn(Collections.emptyList());
      when(taskRepository.countByCycleId(anyLong())).thenReturn(0);
      when(taskRepository.countByCycleIdAndStatus(anyLong(), any())).thenReturn(0);
      when(riskAnalysisService.analyzePitchRisk(any(Pitch.class), anyBoolean()))
          .thenReturn(createPitchRisk(1L, RiskLevel.LOW, 15));

      // When
      EnhancedCycleReportDTO report = reportService.getEnhancedCycleReport(1L);

      // Then
      assertThat(report.getTotalAppetiteHours()).isEqualTo(40.0); // 5 days * 8h
      assertThat(report.getTotalActualHours()).isEqualTo(35.0);
      assertThat(report.getVarianceHours()).isEqualTo(-5.0); // Under budget
      assertThat(report.getVariancePercentage()).isCloseTo(-12.5, within(0.1));
      assertThat(report.getOverBudgetPitches()).isEmpty();
    }
  }

  // Helper method
  private PitchRiskDTO createPitchRisk(Long pitchId, RiskLevel level, int score) {
    return PitchRiskDTO.builder().pitchId(pitchId).pitchTitle("Pitch " + pitchId).riskLevel(level).riskScore(score)
        .riskFactors(Collections.emptyList()).insights(Collections.emptyList())
        .recommendations(Collections.emptyList()).confidenceScore(85).analyzedAt(LocalDateTime.now())
        .aiEnabled(false).build();
  }

  private org.assertj.core.data.Offset<Double> within(double offset) {
    return org.assertj.core.data.Offset.offset(offset);
  }
}
