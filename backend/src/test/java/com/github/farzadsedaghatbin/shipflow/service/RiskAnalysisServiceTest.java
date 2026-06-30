package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskFactor;
import com.github.farzadsedaghatbin.shipflow.dto.risk.RiskScoreExplanation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link RiskAnalysisService#buildScoreExplanation}: verifies the
 * threshold legend, active-band flagging, and per-factor weighted contributions
 * surfaced to the UI.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RiskAnalysisServiceTest {

  @Mock
  private AIConfig aiConfig;
  @Mock
  private PitchRepository pitchRepository;
  @Mock
  private CycleRepository cycleRepository;
  @Mock
  private WorkLogRepository workLogRepository;
  @Mock
  private AICacheService cacheService;
  @Mock
  private PitchRiskHistoryRepository riskHistoryRepository;
  @Mock
  private OrganizationSettingsService organizationSettingsService;
  @Mock
  private RiskHistoryService riskHistoryService;
  @Mock
  private CapacityConfigService capacityConfigService;
  @Mock
  private EpicRepository epicRepository;
  @Mock
  private InitiativeRepository initiativeRepository;

  private RiskAnalysisService service;

  @BeforeEach
  void setUp() {
    service = new RiskAnalysisService(aiConfig, null, pitchRepository, cycleRepository, workLogRepository,
        cacheService, riskHistoryRepository, organizationSettingsService, riskHistoryService,
        capacityConfigService, epicRepository, initiativeRepository);
  }

  private void stubThresholds(int lowMax, int mediumMax, int highMax) {
    OrganizationSettingsDTO.RiskThresholds thresholds = OrganizationSettingsDTO.RiskThresholds.builder()
        .lowMax(lowMax).mediumMax(mediumMax).highMax(highMax).build();
    OrganizationSettingsDTO settings = OrganizationSettingsDTO.builder().riskThresholds(thresholds).build();
    when(organizationSettingsService.getSettings()).thenReturn(settings);
  }

  @Test
  @DisplayName("legend exposes all four bands with contiguous numeric ranges")
  void buildScoreExplanation_exposesFourBands() {
    stubThresholds(30, 60, 85);

    RiskScoreExplanation explanation = service.buildScoreExplanation(46, List.of());

    assertThat(explanation.getScore()).isEqualTo(46);
    assertThat(explanation.getBands()).hasSize(4);
    assertThat(explanation.getBands()).extracting(RiskScoreExplanation.RiskBand::getLevel)
        .containsExactly(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL);

    // Ranges: LOW 0-30, MEDIUM 31-60, HIGH 61-85, CRITICAL 86-100
    assertThat(explanation.getBands().get(0).getMinScore()).isZero();
    assertThat(explanation.getBands().get(0).getMaxScore()).isEqualTo(30);
    assertThat(explanation.getBands().get(1).getMinScore()).isEqualTo(31);
    assertThat(explanation.getBands().get(1).getMaxScore()).isEqualTo(60);
    assertThat(explanation.getBands().get(2).getMinScore()).isEqualTo(61);
    assertThat(explanation.getBands().get(2).getMaxScore()).isEqualTo(85);
    assertThat(explanation.getBands().get(3).getMinScore()).isEqualTo(86);
    assertThat(explanation.getBands().get(3).getMaxScore()).isEqualTo(100);
  }

  @Test
  @DisplayName("exactly the band containing the score is marked active")
  void buildScoreExplanation_marksActiveBand() {
    stubThresholds(30, 60, 85);

    RiskScoreExplanation explanation = service.buildScoreExplanation(46, List.of());

    assertThat(explanation.getActiveBand()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(explanation.getBands()).filteredOn(RiskScoreExplanation.RiskBand::isActive)
        .extracting(RiskScoreExplanation.RiskBand::getLevel).containsExactly(RiskLevel.MEDIUM);
  }

  @Test
  @DisplayName("active band respects configurable (non-default) thresholds")
  void buildScoreExplanation_usesConfigurableThresholds() {
    // Tighter bands: 46 should now be HIGH, not MEDIUM
    stubThresholds(20, 40, 70);

    RiskScoreExplanation explanation = service.buildScoreExplanation(46, List.of());

    assertThat(explanation.getActiveBand()).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  @DisplayName("falls back to default bands when settings lookup fails")
  void buildScoreExplanation_fallsBackToDefaults() {
    when(organizationSettingsService.getSettings()).thenThrow(new RuntimeException("settings unavailable"));

    RiskScoreExplanation explanation = service.buildScoreExplanation(90, List.of());

    // Defaults: LOW<=30, MEDIUM<=60, HIGH<=85, CRITICAL>85
    assertThat(explanation.getActiveBand()).isEqualTo(RiskLevel.CRITICAL);
    assertThat(explanation.getBands().get(0).getMaxScore()).isEqualTo(30);
    assertThat(explanation.getBands().get(2).getMaxScore()).isEqualTo(85);
  }

  @Test
  @DisplayName("each factor's weighted points mirror impact*probability/10, heaviest first")
  void buildScoreExplanation_computesFactorContributions() {
    stubThresholds(30, 60, 85);

    RiskFactor timeOverrun = RiskFactor.builder().category(RiskFactor.RiskCategory.TIME_OVERRUN)
        .description("Cycle is 79% ahead of pitch progress").impactLevel(8).probability(7).build();
    RiskFactor unclear = RiskFactor.builder().category(RiskFactor.RiskCategory.UNCLEAR_REQUIREMENTS)
        .description("Missing context").impactLevel(5).probability(6).build();

    RiskScoreExplanation explanation = service.buildScoreExplanation(46, List.of(unclear, timeOverrun));

    assertThat(explanation.getFactorContributions()).hasSize(2);
    // 8*7/10 = 5.6 should sort before 5*6/10 = 3.0
    RiskScoreExplanation.FactorContribution top = explanation.getFactorContributions().get(0);
    assertThat(top.getCategory()).isEqualTo(RiskFactor.RiskCategory.TIME_OVERRUN);
    assertThat(top.getWeightedPoints()).isEqualTo(5.6);
    assertThat(explanation.getFactorContributions().get(1).getWeightedPoints()).isEqualTo(3.0);
  }

  @Test
  @DisplayName("null factor list yields an empty contributions list, not an error")
  void buildScoreExplanation_handlesNullFactors() {
    stubThresholds(30, 60, 85);

    RiskScoreExplanation explanation = service.buildScoreExplanation(20, null);

    assertThat(explanation.getFactorContributions()).isEmpty();
    assertThat(explanation.getActiveBand()).isEqualTo(RiskLevel.LOW);
  }
}
