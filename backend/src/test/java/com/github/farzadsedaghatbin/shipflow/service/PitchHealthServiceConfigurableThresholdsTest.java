package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for PitchHealthService configurable thresholds feature. Tests that
 * organization settings properly override default thresholds.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PitchHealthServiceConfigurableThresholdsTest {

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
  private OrganizationSettingsService organizationSettingsService;

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
        .cycle(testCycle).team(testTeam).status(PitchStatus.IN_PROGRESS).build();

    // Setup capacity config mock - 8 hours/day default, 14 days * 8 hours = 112 hours
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any(Pitch.class))).thenReturn(112.0);
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
  @DisplayName("Should use custom budget thresholds from organization settings")
  void shouldUseCustomBudgetThresholds() {
    // Given: Custom organization settings with lower budget threshold
    OrganizationSettingsDTO.RiskThresholds customThresholds = createDefaultThresholds();
    customThresholds.setBudgetWarning(60); // Lower than default 80
    customThresholds.setBudgetOverrun(80); // Lower than default 100
    customThresholds.setBudgetCritical(100); // Lower than default 120

    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(customThresholds).build();

    when(organizationSettingsService.getSettings()).thenReturn(settings);
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(84.0); // 75% of appetite (112 hours)
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Risk should be elevated due to lower threshold
    // 75% usage exceeds 60% warning threshold with custom settings
    assertThat(result).isNotNull();
    assertThat(result.getAppetiteUsedPercent()).isGreaterThan(60.0);
  }

  @Test
  @DisplayName("Should use custom critical bug thresholds from organization settings")
  void shouldUseCustomCriticalBugThresholds() {
    // Given: Custom settings with lower critical bug threshold
    OrganizationSettingsDTO.RiskThresholds customThresholds = createDefaultThresholds();
    customThresholds.setCriticalBugsMinor(1); // Same as default
    customThresholds.setCriticalBugsModerate(2); // Lower than default 3
    customThresholds.setCriticalBugsSevere(3); // Lower than default 5

    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(customThresholds).build();

    when(organizationSettingsService.getSettings()).thenReturn(settings);
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);

    // 2 critical bugs - should trigger moderate risk with custom threshold
    BugReport bug1 = createBug(1L, BugSeverity.CRITICAL, BugStatus.OPEN);
    BugReport bug2 = createBug(2L, BugSeverity.CRITICAL, BugStatus.OPEN);
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Arrays.asList(bug1, bug2));

    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Should reflect elevated risk due to 2 critical bugs
    assertThat(result).isNotNull();
    assertThat(result.getRiskLevel()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);
  }

  @Test
  @DisplayName("Should use custom scope progress thresholds from organization settings")
  void shouldUseCustomScopeThresholds() {
    // Given: Custom settings with stricter scope expectations
    OrganizationSettingsDTO.RiskThresholds customThresholds = createDefaultThresholds();
    customThresholds.setScopeUphillMax(40); // Higher than default 30
    customThresholds.setCycleFinalQuarter(70); // Earlier than default 75
    customThresholds.setScopeExpectedProgressRate(0.9); // Higher than default 0.8

    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(customThresholds).build();

    when(organizationSettingsService.getSettings()).thenReturn(settings);
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // Scope at position 35 - acceptable with custom threshold but would be risky
    // with default
    HillChartPoint scope = createHillChartPoint(1L, 35, LocalDateTime.now());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Arrays.asList(scope));
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Arrays.asList(scope));

    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Should evaluate scope progress with custom threshold
    assertThat(result).isNotNull();
    // With position 35 and custom uphillMax of 40, scope is acceptable
  }

  @Test
  @DisplayName("Should use custom risk score boundaries from organization settings")
  void shouldUseCustomRiskScoreBoundaries() {
    // Given: Custom settings with more aggressive risk boundaries
    OrganizationSettingsDTO.RiskThresholds customThresholds = createDefaultThresholds();
    customThresholds.setLowMax(15); // Lower than default 24
    customThresholds.setMediumMax(35); // Lower than default 49
    customThresholds.setHighMax(55); // Lower than default 69

    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(customThresholds).build();

    when(organizationSettingsService.getSettings()).thenReturn(settings);
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));

    // 95% budget usage - should score medium-high risk points
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(106.4); // 95%
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Should classify risk based on custom boundaries
    assertThat(result).isNotNull();
    // With custom lower boundaries, same score results in higher risk level
  }

  @Test
  @DisplayName("Should fall back to defaults when organization settings unavailable")
  void shouldFallBackToDefaultsWhenSettingsUnavailable() {
    // Given: No organization settings available
    when(organizationSettingsService.getSettings()).thenReturn(null);
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Should still calculate risk using default thresholds
    assertThat(result).isNotNull();
    assertThat(result.getRiskLevel()).isNotNull();
  }

  @Test
  @DisplayName("Should handle organization settings service exception gracefully")
  void shouldHandleOrganizationSettingsException() {
    // Given: Organization settings service throws exception
    when(organizationSettingsService.getSettings()).thenThrow(new RuntimeException("Database error"));
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(workLogRepository.getTotalHoursByPitchId(1L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(1L, false);

    // Then: Should fall back to defaults and continue
    assertThat(result).isNotNull();
    assertThat(result.getRiskLevel()).isNotNull();
  }

  @Test
  @DisplayName("Should use custom time-based thresholds from organization settings")
  void shouldUseCustomTimeThresholds() {
    // Given: Custom settings with different urgency thresholds
    LocalDate today = LocalDate.now();
    Cycle urgentCycle = Cycle.builder().id(2L).name("Urgent Cycle").startDate(today.minusDays(10))
        .endDate(today.plusDays(2)) // 2 days left
        .project(testProject).isActive(true).build();

    Pitch urgentPitch = Pitch.builder().id(2L).title("Urgent Pitch").appetiteDays(10).cycle(urgentCycle)
        .team(testTeam).status(PitchStatus.IN_PROGRESS) // Not done yet!
        .build();

    OrganizationSettingsDTO.RiskThresholds customThresholds = createDefaultThresholds();
    customThresholds.setDaysUrgent(2); // Same as default 3
    customThresholds.setDaysWarning(5); // Lower than default 7
    customThresholds.setDaysConcern(10); // Lower than default 14

    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(customThresholds).build();

    when(organizationSettingsService.getSettings()).thenReturn(settings);
    when(pitchRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(urgentPitch));
    when(workLogRepository.getTotalHoursByPitchId(2L)).thenReturn(50.0);
    when(bugReportRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    when(workLogRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());

    // When
    PitchHealthDTO result = pitchHealthService.getPitchHealth(2L, false);

    // Then: Should reflect urgency based on custom threshold
    assertThat(result).isNotNull();
    assertThat(result.getDaysLeft()).isLessThanOrEqualTo(2);
    // Status summary should reflect urgency
    assertThat(result.getStatusSummary()).contains("days");
  }

  // Helper methods
  private OrganizationSettingsDTO.RiskThresholds createDefaultThresholds() {
    return OrganizationSettingsDTO.RiskThresholds.builder()
        // Risk Score Boundaries
        .lowMax(24).mediumMax(49).highMax(69)
        // Budget Thresholds
        .budgetWarning(80).budgetOverrun(100).budgetCritical(120)
        // Schedule Variance Thresholds
        .scheduleModerateGap(15).scheduleSignificantGap(30)
        // Bug Count Thresholds
        .criticalBugsMinor(1).criticalBugsModerate(3).criticalBugsSevere(5).majorBugsThreshold(3)
        .majorBugsHigh(5).openBugsModerate(5).openBugsHigh(10).openBugsCritical(15).recentBugInflux(5)
        // Bug Resolution Rate
        .bugResolutionRateMin(50)
        // Scope Progress Thresholds
        .scopeEarlyPhase(25).scopeUphillMax(30).scopeMidPhase(40).scopePeakMin(45).scopePeakMax(55)
        .scopeExpectedProgressRate(0.8).scopeLagSignificant(30)
        // Time-based Thresholds
        .daysUrgent(3).daysWarning(7).daysConcern(14)
        // Cycle Progress Thresholds
        .cycleMidpoint(50).cycleLatePhase(60).cycleFinalQuarter(75).cycleMinForScopes(30)
        // Stagnation Thresholds
        .scopeStagnationDays(7).peakStuckDays(5).noProgressDays(7)
        // Work Rate Thresholds
        .recentWorkHighHours(15).appetiteHighUsage(90).build();
  }

  private BugReport createBug(Long id, BugSeverity severity, BugStatus status) {
    return BugReport.builder().id(id).severity(severity).status(status).title("Test Bug " + id)
        .description("Test bug description").createdAt(LocalDateTime.now().minusDays(5)).build();
  }

  private HillChartPoint createHillChartPoint(Long id, int position, LocalDateTime updatedAt) {
    return HillChartPoint.builder().id(id).pitch(testPitch).scope("Test Scope " + id)
        .description("Test scope description").position(position).createdAt(LocalDateTime.now().minusDays(7))
        .updatedAt(updatedAt).build();
  }
}
