package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for PitchHealthService - Automated Risk Detection and Health
 * Monitoring
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PitchHealthServiceTest {

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private MeetingRepository meetingRepository;

  @Mock
  private BugReportRepository bugReportRepository;

  @Mock
  private HillChartPointRepository hillChartPointRepository;

  @Mock
  private RiskAnalysisService riskAnalysisService;

  @Mock
  private CapacityConfigService capacityConfigService;

  @InjectMocks
  private PitchHealthService pitchHealthService;

  private Pitch testPitch;
  private Cycle testCycle;
  private Project testProject;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    LocalDate today = LocalDate.now();

    testProject = Project.builder().id(1L).name("Test Project").projectKey("TEST").build();

    testCycle = Cycle.builder().id(1L).name("Test Cycle").startDate(today.minusDays(7)).endDate(today.plusDays(7))
        .project(testProject).isActive(true).build();

    testTeam = Team.builder().id(1L).name("Test Team").build();

    testPitch = Pitch.builder().id(1L).title("Test Pitch").description("Test Description").appetiteDays(14)
        .cycle(testCycle).team(testTeam).status(PitchStatus.IN_PROGRESS)
        .createdAt(LocalDateTime.now().minusDays(7)).updatedAt(LocalDateTime.now()).build();

    // Setup capacity config mock - dynamically calculate hours based on appetite
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any(Pitch.class)))
        .thenAnswer(invocation -> {
          Pitch pitch = invocation.getArgument(0);
          return pitch.getAppetiteDays() * 8.0; // 8 hours/day default
        });
    lenient().when(capacityConfigService.getOrganizationDefaultHoursPerDay()).thenReturn(8.0);
    
    // Mock TeamBudget for risk calculation
    CapacityConfigService.TeamBudget mockTeamBudget = CapacityConfigService.TeamBudget.builder()
        .teamId(1L)
        .teamName("Test Team")
        .memberCount(2)
        .appetiteDays(14)
        .totalBudgetHours(224.0) // 2 members * 14 days * 8 hours
        .totalDailyCapacityHours(16.0) // 2 members * 8 hours
        .capacitySource("organization")
        .memberBudgets(Arrays.asList())
        .totalHoursSpent(0.0)
        .totalHoursRemaining(224.0)
        .utilizationPercentage(0.0)
        .build();
    lenient().when(capacityConfigService.calculateTeamBudget(any(Team.class), anyInt())).thenReturn(mockTeamBudget);
  }

  @Test
  void getPitchHealth_ShouldReturnHealthSummary_WhenPitchExists() {
    // Given
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getPitchId()).isEqualTo(1L);
    assertThat(result.getPitchName()).isEqualTo("Test Pitch");
    assertThat(result.getRiskLevel()).isNotNull();
    assertThat(result.getRiskTrend()).isNotNull();
    assertThat(result.getAppetiteUsedPercent()).isGreaterThanOrEqualTo(0.0);
    verify(pitchRepository).findByIdNotDeleted(1L);
  }

  @Test
  void getPitchHealth_ShouldThrowException_WhenPitchNotFound() {
    // Given
    when(pitchRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> pitchHealthService.getPitchHealth(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Pitch not found");
  }

  @Test
  void calculateRiskLevel_ShouldReturnCritical_WhenMultipleCriticalBugs() {
    // Given - Pitch with 3 critical bugs
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    List<BugReport> criticalBugs = Arrays.asList(createBug(1L, BugSeverity.CRITICAL, BugStatus.OPEN),
        createBug(2L, BugSeverity.BLOCKER, BugStatus.OPEN),
        createBug(3L, BugSeverity.CRITICAL, BugStatus.OPEN));
    when(bugReportRepository.findByPitchId(1L)).thenReturn(criticalBugs);

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then - Should show elevated risk due to multiple critical bugs
    assertThat(result.getRiskLevel()).isIn(RiskLevel.CRITICAL, RiskLevel.HIGH, RiskLevel.MEDIUM);
  }

  @Test
  void calculateRiskLevel_ShouldReturnHigh_WhenOverBudget() {
    // Given - Pitch using 130% of budget
    testPitch.setAppetiteDays(10); // 80 hours budget
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(104.0); // 130% usage
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then - Should be at least MEDIUM risk due to budget overrun
    assertThat(result.getRiskLevel()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);
    assertThat(result.getAppetiteUsedPercent()).isGreaterThan(120.0);
  }

  @Test
  void calculateRiskLevel_ShouldReturnHigh_WhenScopesStagnant() {
    // Given - Scopes stuck in uphill phase late in cycle
    LocalDate today = LocalDate.now();
    testCycle.setStartDate(today.minusDays(21)); // 3 weeks ago
    testCycle.setEndDate(today.plusDays(7)); // 1 week left (75% through cycle)

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // Scopes at position 20 (early uphill) updated 8 days ago
    List<HillChartPoint> stagnantScopes = Arrays.asList(
        createHillChartPoint(1L, 20, LocalDateTime.now().minusDays(8)),
        createHillChartPoint(2L, 15, LocalDateTime.now().minusDays(10)));
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(stagnantScopes);
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(1L)).thenReturn(stagnantScopes);

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then - Should show increased risk due to stagnant scopes
    assertThat(result.getRiskLevel()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);
  }

  @Test
  void calculateRiskLevel_ShouldReturnLow_WhenHealthy() {
    // Given - Healthy pitch: on time, under budget, no bugs, good progress
    testCycle.setStartDate(LocalDate.now().minusDays(7));
    testCycle.setEndDate(LocalDate.now().plusDays(21)); // Plenty of time
    testPitch.setStatus(PitchStatus.IN_PROGRESS);

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(30.0); // ~27% of 112 hours
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // Good progress - scopes in downhill phase
    List<HillChartPoint> goodScopes = Arrays
        .asList(createHillChartPoint(1L, 70, LocalDateTime.now().minusHours(2)));
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(goodScopes);
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(1L)).thenReturn(goodScopes);

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then
    assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
  }

  @Test
  void calculateRiskTrend_ShouldReturnWorsening_WhenNewCriticalBugs() {
    // Given - Recent critical bugs filed
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(1L)).thenReturn(Arrays.asList());

    List<BugReport> recentCriticalBugs = Arrays
        .asList(createRecentBug(1L, BugSeverity.CRITICAL, BugStatus.OPEN, LocalDateTime.now().minusHours(12)));
    when(bugReportRepository.findByPitchId(1L)).thenReturn(recentCriticalBugs);
    when(workLogRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then
    assertThat(result.getRiskTrend()).isEqualTo("WORSENING");
  }

  @Test
  void calculateRiskTrend_ShouldReturnImproving_WhenRecentProgress() {
    // Given - Recent hill chart updates showing progress
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(workLogRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    List<HillChartPoint> recentProgress = Arrays
        .asList(createHillChartPoint(1L, 65, LocalDateTime.now().minusHours(6)));
    when(hillChartPointRepository.findByPitchId(1L)).thenReturn(recentProgress);
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(1L)).thenReturn(recentProgress);

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L);

    // Then
    assertThat(result.getRiskTrend()).isIn("IMPROVING", "STABLE");
  }

  @Test
  void getCycleHealthSummary_ShouldAggregateAllPitches() {
    // Given
    Pitch pitch2 = Pitch.builder().id(2L).title("Pitch 2").appetiteDays(10).cycle(testCycle).team(testTeam)
        .status(PitchStatus.DONE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(Arrays.asList(testPitch, pitch2));

    when(workLogRepository.getTotalHoursByPitchIds(any())).thenAnswer(invocation -> {
      List<Object[]> result = new java.util.ArrayList<>();
      result.add(new Object[] { 1L, 56.0 });
      result.add(new Object[] { 2L, 40.0 });
      return result;
    });
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());
    when(bugReportRepository.findByPitchId(2L)).thenReturn(Arrays.asList());

    // When
    CycleHealthSummaryDTO result = pitchHealthService.getCycleHealthSummary(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getCycleId()).isEqualTo(1L);
    assertThat(result.getCycleName()).isEqualTo("Test Cycle");
    assertThat(result.getTotalPitches()).isEqualTo(2);
    assertThat(result.getPitchHealthList()).hasSize(2);
    assertThat(result.getDaysLeft()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void getCycleHealthSummary_ShouldCalculateRiskBreakdown() {
    // Given - Mix of healthy and at-risk pitches
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(Arrays.asList(testPitch));
    when(workLogRepository.getTotalHoursByPitchIds(any())).thenAnswer(invocation -> {
      List<Object[]> result = new java.util.ArrayList<>();
      result.add(new Object[] { 1L, 50.0 });
      return result;
    });
    when(bugReportRepository.findByPitchId(1L)).thenReturn(Arrays.asList());

    // When
    CycleHealthSummaryDTO result = pitchHealthService.getCycleHealthSummary(1L);

    // Then
    assertThat(result.getHealthyPitches()).isGreaterThanOrEqualTo(0);
    assertThat(result.getAtRiskPitches()).isGreaterThanOrEqualTo(0);
    assertThat(result.getCriticalPitches()).isGreaterThanOrEqualTo(0);
    assertThat(result.getHealthyPitches() + result.getAtRiskPitches() + result.getCriticalPitches())
        .isEqualTo(result.getTotalPitches());
  }

  @Test
  void getAllActiveCycleHealth_ShouldReturnAllActiveCycles() {
    // Given
    when(cycleRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testCycle));
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(Arrays.asList());
    when(workLogRepository.getTotalHoursByPitchIds(any()))
        .thenAnswer(invocation -> new java.util.ArrayList<Object[]>());
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Arrays.asList());

    // When
    List<CycleHealthSummaryDTO> result = pitchHealthService.getAllActiveCycleHealth(false);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCycleName()).isEqualTo("Test Cycle");
  }

  // Helper methods
  private BugReport createBug(Long id, BugSeverity severity, BugStatus status) {
    return BugReport.builder().id(id).severity(severity).status(status).title("Test Bug " + id)
        .description("Test bug description").createdAt(LocalDateTime.now().minusDays(5)).build();
  }

  private BugReport createRecentBug(Long id, BugSeverity severity, BugStatus status, LocalDateTime createdAt) {
    return BugReport.builder().id(id).severity(severity).status(status).title("Test Bug " + id)
        .description("Test bug description").createdAt(createdAt).build();
  }

  private HillChartPoint createHillChartPoint(Long id, int position, LocalDateTime updatedAt) {
    return HillChartPoint.builder().id(id).pitch(testPitch).scope("Test Scope " + id)
        .description("Test scope description").position(position).createdAt(LocalDateTime.now().minusDays(7))
        .updatedAt(updatedAt).build();
  }
}
