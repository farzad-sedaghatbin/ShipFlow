package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.betting.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("BettingDecisionService Tests")
class BettingDecisionServiceTest {

  @Mock
  private BettingDecisionRepository bettingDecisionRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private BettingSlotRepository bettingSlotRepository;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private BettingDecisionService bettingDecisionService;

  private Cycle testCycle;
  private Team testTeam;
  private Pitch testPitch;
  private User testUser;
  private BettingDecision testDecision;

  @BeforeEach
  void setUp() {
    // Setup lenient message service stubs
    lenient().when(messageService.getMessage(anyString(), any(Object.class))).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      Object arg = invocation.getArgument(1);
      return key + ": " + arg;
    });

    testCycle = Cycle.builder()
        .id(1L)
        .name("Q1 2026")
        .phase(CyclePhase.SHAPING_BUILDING)
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .isActive(true)
        .build();

    testTeam = Team.builder()
        .id(1L)
        .name("Alpha Team")
        .build();

    testPitch = Pitch.builder()
        .id(1L)
        .title("User Authentication Feature")
        .description("Implement OAuth2 login")
        .appetiteDays(14)
        .status(PitchStatus.SHAPED)
        .cycle(testCycle)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testUser = User.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .password("password")
        .role(UserRole.ADMIN)
        .isActive(true)
        .build();

    testDecision = BettingDecision.builder()
        .id(1L)
        .pitch(testPitch)
        .cycle(testCycle)
        .decision(BettingDecisionType.COMMITTED)
        .reason("High strategic value")
        .decidedBy(testUser)
        .decidedAt(LocalDateTime.now())
        .requestedAppetiteDays(14)
        .availableCapacityDays(21)
        .consideredTeam(testTeam)
        .priorityScore(85)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Nested
  @DisplayName("Record Decision Tests")
  class RecordDecisionTests {

    @Test
    @DisplayName("Should successfully record a COMMITTED decision")
    void recordDecision_Committed_ShouldSucceed() {
      // Given
      RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
          .pitchId(1L)
          .cycleId(1L)
          .decision(BettingDecisionType.COMMITTED)
          .reason("High strategic value, team has capacity")
          .consideredTeamId(1L)
          .availableCapacityDays(21)
          .appetiteComparisonNotes("Fits well")
          .priorityScore(85)
          .build();

      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
      when(bettingDecisionRepository.save(any(BettingDecision.class))).thenReturn(testDecision);

      // When
      BettingDecisionDTO result = bettingDecisionService.recordDecision(request, 1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
      assertThat(result.getPitchTitle()).isEqualTo("User Authentication Feature");

      ArgumentCaptor<BettingDecision> captor = ArgumentCaptor.forClass(BettingDecision.class);
      verify(bettingDecisionRepository).save(captor.capture());
      BettingDecision saved = captor.getValue();
      assertThat(saved.getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
      assertThat(saved.getReason()).isEqualTo("High strategic value, team has capacity");
    }

    @Test
    @DisplayName("Should successfully record a REJECTED decision")
    void recordDecision_Rejected_ShouldSucceed() {
      // Given
      RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
          .pitchId(1L)
          .cycleId(1L)
          .decision(BettingDecisionType.REJECTED)
          .reason("Appetite exceeds available capacity")
          .build();

      BettingDecision rejectedDecision = BettingDecision.builder()
          .id(2L)
          .pitch(testPitch)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Appetite exceeds available capacity")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(14)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(bettingDecisionRepository.save(any(BettingDecision.class))).thenReturn(rejectedDecision);

      // When
      BettingDecisionDTO result = bettingDecisionService.recordDecision(request, 1L);

      // Then
      assertThat(result.getDecision()).isEqualTo(BettingDecisionType.REJECTED);
    }

    @Test
    @DisplayName("Should throw exception when pitch not found")
    void recordDecision_PitchNotFound_ShouldThrow() {
      // Given
      RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
          .pitchId(999L)
          .cycleId(1L)
          .decision(BettingDecisionType.COMMITTED)
          .reason("Test")
          .build();

      when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> bettingDecisionService.recordDecision(request, 1L))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when cycle not found")
    void recordDecision_CycleNotFound_ShouldThrow() {
      // Given
      RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
          .pitchId(1L)
          .cycleId(999L)
          .decision(BettingDecisionType.COMMITTED)
          .reason("Test")
          .build();

      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> bettingDecisionService.recordDecision(request, 1L))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void recordDecision_UserNotFound_ShouldThrow() {
      // Given
      RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
          .pitchId(1L)
          .cycleId(1L)
          .decision(BettingDecisionType.COMMITTED)
          .reason("Test")
          .build();

      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> bettingDecisionService.recordDecision(request, 999L))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Get Decision History Tests")
  class GetHistoryTests {

    @Test
    @DisplayName("Should get pitch decision history")
    void getPitchDecisionHistory_ShouldReturnHistory() {
      // Given
      List<BettingDecision> decisions = Arrays.asList(testDecision);
      when(bettingDecisionRepository.findByPitchIdOrderByDecidedAtDesc(1L)).thenReturn(decisions);

      // When
      List<BettingDecisionDTO> result = bettingDecisionService.getPitchDecisionHistory(1L);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getPitchId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get cycle decisions")
    void getCycleDecisions_ShouldReturnDecisions() {
      // Given
      List<BettingDecision> decisions = Arrays.asList(testDecision);
      when(bettingDecisionRepository.findByCycleIdOrderByDecidedAtDesc(1L)).thenReturn(decisions);

      // When
      List<BettingDecisionDTO> result = bettingDecisionService.getCycleDecisions(1L);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCycleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get most recent decision for pitch")
    void getMostRecentDecision_ShouldReturnLatest() {
      // Given
      when(bettingDecisionRepository.findFirstByPitchIdOrderByDecidedAtDesc(1L))
          .thenReturn(Optional.of(testDecision));

      // When
      Optional<BettingDecisionDTO> result = bettingDecisionService.getMostRecentDecision(1L);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
    }

    @Test
    @DisplayName("Should return empty when no decision exists")
    void getMostRecentDecision_WhenNone_ShouldReturnEmpty() {
      // Given
      when(bettingDecisionRepository.findFirstByPitchIdOrderByDecidedAtDesc(1L))
          .thenReturn(Optional.empty());

      // When
      Optional<BettingDecisionDTO> result = bettingDecisionService.getMostRecentDecision(1L);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Cycle Decision History Tests")
  class CycleDecisionHistoryTests {

    @Test
    @DisplayName("Should get comprehensive cycle decision history")
    void getCycleDecisionHistory_ShouldReturnFullHistory() {
      // Given
      BettingDecision rejectedDecision = BettingDecision.builder()
          .id(2L)
          .pitch(testPitch)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Over capacity")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(14)
          .availableCapacityDays(10)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      List<BettingDecision> decisions = Arrays.asList(testDecision, rejectedDecision);

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(bettingDecisionRepository.findByCycleIdOrderByDecidedAtDesc(1L)).thenReturn(decisions);
      when(bettingDecisionRepository.findRepeatedlyRejectedPitches(2)).thenReturn(new ArrayList<>());

      // When
      BettingDecisionHistoryDTO result = bettingDecisionService.getCycleDecisionHistory(1L);

      // Then
      assertThat(result.getTotalDecisions()).isEqualTo(2);
      assertThat(result.getCommittedCount()).isEqualTo(1);
      assertThat(result.getRejectedCount()).isEqualTo(1);
      assertThat(result.getCycleName()).isEqualTo("Q1 2026");
    }

    @Test
    @DisplayName("Should throw when cycle not found")
    void getCycleDecisionHistory_CycleNotFound_ShouldThrow() {
      // Given
      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> bettingDecisionService.getCycleDecisionHistory(999L))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Appetite Comparison Tests")
  class AppetiteComparisonTests {

    @Test
    @DisplayName("Should compare appetite - comfortable fit")
    void compareAppetite_ComfortableFit_ShouldReturnCorrectAssessment() {
      // Given
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(bettingSlotRepository.findByCycleIdAndTeamId(1L, 1L)).thenReturn(new ArrayList<>());

      // When
      AppetiteComparisonDTO result = bettingDecisionService.compareAppetite(1L, 1L, 1L);

      // Then
      assertThat(result.getFits()).isTrue();
      assertThat(result.getRequestedAppetiteDays()).isEqualTo(14);
      assertThat(result.getAssessment()).isIn(
          AppetiteComparisonDTO.FitAssessment.COMFORTABLE,
          AppetiteComparisonDTO.FitAssessment.GOOD_FIT,
          AppetiteComparisonDTO.FitAssessment.TIGHT_FIT);
    }

    @Test
    @DisplayName("Should compare appetite for all teams")
    void compareAppetiteAllTeams_ShouldReturnSortedList() {
      // Given
      Team team2 = Team.builder().id(2L).name("Beta Team").build();
      List<Team> teams = Arrays.asList(testTeam, team2);

      when(teamRepository.findByCycleId(1L)).thenReturn(teams);
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(teamRepository.findById(anyLong())).thenAnswer(invocation -> {
        Long id = invocation.getArgument(0);
        return id == 1L ? Optional.of(testTeam) : Optional.of(team2);
      });
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(bettingSlotRepository.findByCycleIdAndTeamId(eq(1L), anyLong())).thenReturn(new ArrayList<>());

      // When
      List<AppetiteComparisonDTO> result = bettingDecisionService.compareAppetiteAllTeams(1L, 1L);

      // Then
      assertThat(result).hasSize(2);
      // Should be sorted by utilization percentage
      assertThat(result.get(0).getUtilizationPercentage())
          .isLessThanOrEqualTo(result.get(1).getUtilizationPercentage());
    }

    @Test
    @DisplayName("Should throw when pitch not found for comparison")
    void compareAppetite_PitchNotFound_ShouldThrow() {
      // Given
      when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> bettingDecisionService.compareAppetite(999L, 1L, 1L))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Decision Query Tests")
  class DecisionQueryTests {

    @Test
    @DisplayName("Should check if decision exists for pitch in cycle")
    void hasDecisionForPitchInCycle_ShouldReturnTrue() {
      // Given
      when(bettingDecisionRepository.existsByPitchIdAndCycleId(1L, 1L)).thenReturn(true);

      // When
      boolean result = bettingDecisionService.hasDecisionForPitchInCycle(1L, 1L);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when no decision exists")
    void hasDecisionForPitchInCycle_NoDecision_ShouldReturnFalse() {
      // Given
      when(bettingDecisionRepository.existsByPitchIdAndCycleId(1L, 1L)).thenReturn(false);

      // When
      boolean result = bettingDecisionService.hasDecisionForPitchInCycle(1L, 1L);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get committed decisions for cycle")
    void getCommittedDecisions_ShouldReturnCommitted() {
      // Given
      when(bettingDecisionRepository.findCommittedByCycleId(1L))
          .thenReturn(Arrays.asList(testDecision));

      // When
      List<BettingDecisionDTO> result = bettingDecisionService.getCommittedDecisions(1L);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
    }

    @Test
    @DisplayName("Should get rejected decisions for cycle")
    void getRejectedDecisions_ShouldReturnRejected() {
      // Given
      BettingDecision rejected = BettingDecision.builder()
          .id(2L)
          .pitch(testPitch)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Over capacity")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(14)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      when(bettingDecisionRepository.findRejectedByCycleId(1L))
          .thenReturn(Arrays.asList(rejected));

      // When
      List<BettingDecisionDTO> result = bettingDecisionService.getRejectedDecisions(1L);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getDecision()).isEqualTo(BettingDecisionType.REJECTED);
    }

    @Test
    @DisplayName("Should get paginated project decisions")
    void getProjectDecisions_ShouldReturnPage() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      Page<BettingDecision> page = new PageImpl<>(Arrays.asList(testDecision));
      when(bettingDecisionRepository.findByProjectId(1L, pageable)).thenReturn(page);

      // When
      Page<BettingDecisionDTO> result = bettingDecisionService.getProjectDecisions(1L, pageable);

      // Then
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).getPitchId()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("AppetiteComparisonDTO Static Methods Tests")
  class AppetiteComparisonStaticTests {

    @Test
    @DisplayName("Should calculate COMFORTABLE assessment for low utilization")
    void calculateAssessment_LowUtilization_ShouldBeComfortable() {
      assertThat(AppetiteComparisonDTO.calculateAssessment(50))
          .isEqualTo(AppetiteComparisonDTO.FitAssessment.COMFORTABLE);
    }

    @Test
    @DisplayName("Should calculate GOOD_FIT assessment for moderate utilization")
    void calculateAssessment_ModerateUtilization_ShouldBeGoodFit() {
      assertThat(AppetiteComparisonDTO.calculateAssessment(80))
          .isEqualTo(AppetiteComparisonDTO.FitAssessment.GOOD_FIT);
    }

    @Test
    @DisplayName("Should calculate TIGHT_FIT assessment for high utilization")
    void calculateAssessment_HighUtilization_ShouldBeTightFit() {
      assertThat(AppetiteComparisonDTO.calculateAssessment(95))
          .isEqualTo(AppetiteComparisonDTO.FitAssessment.TIGHT_FIT);
    }

    @Test
    @DisplayName("Should calculate NEEDS_SCOPE_TRIM for slight over capacity")
    void calculateAssessment_SlightOverCapacity_ShouldNeedScopeTrim() {
      assertThat(AppetiteComparisonDTO.calculateAssessment(110))
          .isEqualTo(AppetiteComparisonDTO.FitAssessment.NEEDS_SCOPE_TRIM);
    }

    @Test
    @DisplayName("Should calculate WONT_FIT for significant over capacity")
    void calculateAssessment_SignificantOverCapacity_ShouldNotFit() {
      assertThat(AppetiteComparisonDTO.calculateAssessment(150))
          .isEqualTo(AppetiteComparisonDTO.FitAssessment.WONT_FIT);
    }

    @Test
    @DisplayName("Should generate appropriate recommendations")
    void generateRecommendation_ShouldReturnAppropriateText() {
      String recommendation = AppetiteComparisonDTO.generateRecommendation(
          AppetiteComparisonDTO.FitAssessment.COMFORTABLE, 14, 42);
      assertThat(recommendation).contains("Excellent fit");
      assertThat(recommendation).contains("14 days");
      assertThat(recommendation).contains("42 days");
    }
  }
}
