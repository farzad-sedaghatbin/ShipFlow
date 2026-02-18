package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.cycle.PhaseTransitionResultDTO;
import com.github.farzadsedaghatbin.shipflow.dto.cycle.PhaseTransitionResultDTO.WarningCategory;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CycleServicePhaseTransitionTest {

  @Mock
  private CycleRepository cycleRepository;
  @Mock
  private ProjectRepository projectRepository;
  @Mock
  private PitchRepository pitchRepository;
  @Mock
  private RetroRepository retroRepository;
  @Mock
  private BettingDecisionRepository bettingDecisionRepository;
  @Mock
  private DashboardNotificationService notificationService;
  @Mock
  private UserRepository userRepository;
  @Mock
  private OrganizationSettingsService organizationSettingsService;
  @Mock
  private MessageService messageService;

  @InjectMocks
  private CycleService cycleService;

  private Cycle testCycle;
  private Project testProject;

  @BeforeEach
  void setUp() {
    testProject = Project.builder()
        .id(1L)
        .name("Test Project")
        .projectKey("TEST")
        .enableRetrospectives(true)
        .build();

    testCycle = Cycle.builder()
        .id(1L)
        .name("Test Cycle Q1")
        .project(testProject)
        .phase(CyclePhase.SHAPING)
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .isActive(true)
        .build();
  }

  @Nested
  @DisplayName("Transition from SHAPING")
  class ShapingTransitionTests {

    @Test
    @DisplayName("Should succeed with no warnings when pitches are shaped")
    void transitionFromShaping_WithShapedPitches_NoWarnings() {
      testCycle.setPhase(CyclePhase.SHAPING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(3L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BETTING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.getPreviousPhase()).isEqualTo(CyclePhase.SHAPING);
      assertThat(result.getNewPhase()).isEqualTo(CyclePhase.BETTING);
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("Should warn when no pitches have been shaped")
    void transitionFromShaping_NoPitchesShaped_Warning() {
      testCycle.setPhase(CyclePhase.SHAPING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(0L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BETTING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.EMPTY_CYCLE);
    }
  }

  @Nested
  @DisplayName("Transition from BETTING")
  class BettingTransitionTests {

    @Test
    @DisplayName("Should succeed with no warnings when all pitches have decisions")
    void transitionFromBetting_AllDecisionsMade_NoWarnings() {
      testCycle.setPhase(CyclePhase.BETTING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(5L);
      when(bettingDecisionRepository.countByCycleId(1L)).thenReturn(5L);
      when(bettingDecisionRepository.countCommittedByCycleId(1L)).thenReturn(3L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BUILD);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.getNewPhase()).isEqualTo(CyclePhase.BUILD);
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("Should warn when pitches have no decisions")
    void transitionFromBetting_MissingDecisions_Warning() {
      testCycle.setPhase(CyclePhase.BETTING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(5L);
      when(bettingDecisionRepository.countByCycleId(1L)).thenReturn(2L); // Only 2 decisions for 5 pitches
      when(bettingDecisionRepository.countCommittedByCycleId(1L)).thenReturn(2L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BUILD);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.BETTING_INCOMPLETE && w.getAffectedCount() == 3);
    }

    @Test
    @DisplayName("Should warn when no pitches are committed")
    void transitionFromBetting_NoCommittedPitches_Warning() {
      testCycle.setPhase(CyclePhase.BETTING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(3L);
      when(bettingDecisionRepository.countByCycleId(1L)).thenReturn(3L);
      when(bettingDecisionRepository.countCommittedByCycleId(1L)).thenReturn(0L); // All rejected
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BUILD);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.UNCOMMITTED_PITCHES);
    }
  }

  @Nested
  @DisplayName("Transition from BUILD")
  class BuildTransitionTests {

    @Test
    @DisplayName("Should succeed with no warnings when all work is complete")
    void transitionFromBuild_AllComplete_NoWarnings() {
      testCycle.setPhase(CyclePhase.BUILD);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.COOLDOWN);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.getNewPhase()).isEqualTo(CyclePhase.COOLDOWN);
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("Should warn when pitches are incomplete")
    void transitionFromBuild_IncompletePitches_Warning() {
      testCycle.setPhase(CyclePhase.BUILD);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatusIn(eq(1L), anyList())).thenReturn(2L);
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.COOLDOWN);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.PITCHES_NOT_COMPLETE && w.getAffectedCount() == 2);
    }

    @Test
    @DisplayName("Should warn when no retrospective exists")
    void transitionFromBuild_NoRetrospective_Warning() {
      testCycle.setPhase(CyclePhase.BUILD);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
      when(retroRepository.countByCycleId(1L)).thenReturn(0L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.COOLDOWN);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.NO_RETROSPECTIVE);
    }

    @Test
    @DisplayName("Should not warn about retrospective when disabled for project")
    void transitionFromBuild_RetroDisabled_NoRetroWarning() {
      testProject.setEnableRetrospectives(false);
      testCycle.setPhase(CyclePhase.BUILD);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.COOLDOWN);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isFalse();
    }
  }

  @Nested
  @DisplayName("Transition from COOLDOWN")
  class CooldownTransitionTests {

    @Test
    @DisplayName("Should succeed with no warnings when retrospective is closed")
    void transitionFromCooldown_RetroClosed_NoWarnings() {
      testCycle.setPhase(CyclePhase.COOLDOWN);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(1L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.SHAPING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.getNewPhase()).isEqualTo(CyclePhase.SHAPING);
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("Should warn when retrospective is not closed")
    void transitionFromCooldown_RetroNotClosed_Warning() {
      testCycle.setPhase(CyclePhase.COOLDOWN);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);
      when(retroRepository.countByCycleId(1L)).thenReturn(1L); // Open retro exists
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.SHAPING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCode().equals("RETROSPECTIVE_NOT_CLOSED"));
    }
  }

  @Nested
  @DisplayName("Skipped Phase Detection")
  class SkippedPhaseTests {

    @Test
    @DisplayName("Should warn when skipping phases forward")
    void transitionSkippingForward_ShouldWarn() {
      testCycle.setPhase(CyclePhase.SHAPING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(3L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      // Skip from SHAPING directly to BUILD (skipping BETTING)
      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BUILD);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.SKIPPED_PHASE);
    }

    @Test
    @DisplayName("Should warn when going backwards")
    void transitionBackwards_ShouldWarn() {
      testCycle.setPhase(CyclePhase.BUILD);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      // Go backwards from BUILD to BETTING
      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.BETTING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).anyMatch(
          w -> w.getCategory() == WarningCategory.SKIPPED_PHASE);
    }

    @Test
    @DisplayName("Should not warn for COOLDOWN to SHAPING (normal wrap-around)")
    void transitionCooldownToShaping_NoSkipWarning() {
      testCycle.setPhase(CyclePhase.COOLDOWN);
      testProject.setEnableRetrospectives(false);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

      PhaseTransitionResultDTO result = cycleService.transitionPhase(1L, CyclePhase.SHAPING);

      assertThat(result.isTransitionCompleted()).isTrue();
      assertThat(result.getWarnings().stream()
          .noneMatch(w -> w.getCategory() == WarningCategory.SKIPPED_PHASE)).isTrue();
    }
  }

  @Nested
  @DisplayName("Notifications")
  class NotificationTests {

    @Test
    @DisplayName("Should send notification on phase change")
    void transitionPhase_ShouldNotify() {
      testCycle.setPhase(CyclePhase.SHAPING);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.countByCycleIdAndStatus(1L, PitchStatus.SHAPED)).thenReturn(3L);
      when(cycleRepository.save(any(Cycle.class))).thenAnswer(inv -> {
        Cycle c = inv.getArgument(0);
        c.setPhase(CyclePhase.BETTING);
        return c;
      });

      cycleService.transitionPhase(1L, CyclePhase.BETTING);

      verify(notificationService).notifyCyclePhaseChange(any(Cycle.class), eq(CyclePhase.SHAPING), eq(CyclePhase.BETTING));
    }
  }
}
