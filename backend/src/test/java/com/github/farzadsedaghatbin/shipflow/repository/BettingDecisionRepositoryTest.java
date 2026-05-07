package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.BettingDecision;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class BettingDecisionRepositoryTest {

  @Autowired
  private BettingDecisionRepository bettingDecisionRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private UserRepository userRepository;

  private Cycle testCycle;
  private Cycle testCycle2;
  private Team testTeam;
  private Pitch testPitch;
  private Pitch testPitch2;
  private User testUser;
  private BettingDecision testDecision;

  @BeforeEach
  void setUp() {
    // Clean up
    bettingDecisionRepository.deleteAll();
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    // Create test user
    testUser = User.builder()
        .username("testuser")
        .email("test@example.com")
        .password("password123")
        .role(UserRole.ADMIN)
        .isActive(true)
        .build();
    testUser = userRepository.save(testUser);

    // Create test cycle
    testCycle = Cycle.builder()
        .name("Test Cycle Q1")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true)
        .build();
    testCycle = cycleRepository.save(testCycle);

    // Create second cycle for multi-cycle tests
    testCycle2 = Cycle.builder()
        .name("Test Cycle Q2")
        .startDate(LocalDate.now().plusWeeks(8))
        .endDate(LocalDate.now().plusWeeks(14))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(false)
        .build();
    testCycle2 = cycleRepository.save(testCycle2);

    // Create test team
    testTeam = Team.builder()
        .name("Test Team Alpha")
        .build();
    testTeam = teamRepository.save(testTeam);

    // Create test pitches
    testPitch = Pitch.builder()
        .title("Test Pitch - User Authentication")
        .description("Implement OAuth2 login")
        .appetiteDays(14)
        .cycle(testCycle)
        .team(testTeam)
        .status(PitchStatus.SHAPED)
        .build();
    testPitch = pitchRepository.save(testPitch);

    testPitch2 = Pitch.builder()
        .title("Test Pitch - Dashboard Redesign")
        .description("Redesign main dashboard")
        .appetiteDays(21)
        .cycle(testCycle)
        .status(PitchStatus.SHAPED)
        .build();
    testPitch2 = pitchRepository.save(testPitch2);

    // Create test betting decision
    testDecision = BettingDecision.builder()
        .pitch(testPitch)
        .cycle(testCycle)
        .decision(BettingDecisionType.COMMITTED)
        .reason("High strategic value, team has capacity")
        .decidedBy(testUser)
        .decidedAt(LocalDateTime.now())
        .requestedAppetiteDays(14)
        .availableCapacityDays(21)
        .appetiteComparisonNotes("Fits well within team capacity")
        .consideredTeam(testTeam)
        .priorityScore(85)
        .strategicAlignmentNotes("Aligns with Q1 security initiative")
        .build();
    testDecision = bettingDecisionRepository.save(testDecision);
  }

  @Nested
  @DisplayName("Basic CRUD Operations")
  class BasicCrudTests {

    @Test
    @DisplayName("Should save betting decision with all fields")
    void save_ShouldPersistBettingDecision() {
      BettingDecision newDecision = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Appetite too large for remaining capacity")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(21)
          .availableCapacityDays(14)
          .appetiteComparisonNotes("Exceeds available capacity by 7 days")
          .build();

      BettingDecision saved = bettingDecisionRepository.save(newDecision);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getDecision()).isEqualTo(BettingDecisionType.REJECTED);
      assertThat(saved.getReason()).isEqualTo("Appetite too large for remaining capacity");
      assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find betting decision by ID")
    void findById_WhenExists_ShouldReturnDecision() {
      Optional<BettingDecision> found = bettingDecisionRepository.findById(testDecision.getId());

      assertThat(found).isPresent();
      assertThat(found.get().getReason()).isEqualTo("High strategic value, team has capacity");
    }

    @Test
    @DisplayName("Should delete betting decision")
    void deleteById_ShouldRemoveDecision() {
      bettingDecisionRepository.deleteById(testDecision.getId());

      Optional<BettingDecision> found = bettingDecisionRepository.findById(testDecision.getId());
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("Query by Pitch")
  class QueryByPitchTests {

    @Test
    @DisplayName("Should find all decisions for a pitch ordered by date")
    void findByPitchIdOrderByDecidedAtDesc_ShouldReturnOrderedDecisions() {
      // Add another decision for same pitch in different cycle
      BettingDecision laterDecision = BettingDecision.builder()
          .pitch(testPitch)
          .cycle(testCycle2)
          .decision(BettingDecisionType.DEFERRED)
          .reason("Deferred to next cycle")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now().plusDays(1))
          .requestedAppetiteDays(14)
          .build();
      bettingDecisionRepository.save(laterDecision);

      List<BettingDecision> decisions = bettingDecisionRepository
          .findByPitchIdOrderByDecidedAtDesc(testPitch.getId());

      assertThat(decisions).hasSize(2);
      assertThat(decisions.get(0).getDecision()).isEqualTo(BettingDecisionType.DEFERRED);
      assertThat(decisions.get(1).getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
    }

    @Test
    @DisplayName("Should find most recent decision for a pitch")
    void findFirstByPitchIdOrderByDecidedAtDesc_ShouldReturnMostRecent() {
      Optional<BettingDecision> mostRecent = bettingDecisionRepository
          .findFirstByPitchIdOrderByDecidedAtDesc(testPitch.getId());

      assertThat(mostRecent).isPresent();
      assertThat(mostRecent.get().getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
    }
  }

  @Nested
  @DisplayName("Query by Cycle")
  class QueryByCycleTests {

    @Test
    @DisplayName("Should find all decisions for a cycle")
    void findByCycleIdOrderByDecidedAtDesc_ShouldReturnCycleDecisions() {
      // Add rejected decision for different pitch
      BettingDecision rejectedDecision = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Not aligned with current goals")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(21)
          .build();
      bettingDecisionRepository.save(rejectedDecision);

      List<BettingDecision> decisions = bettingDecisionRepository
          .findByCycleIdOrderByDecidedAtDesc(testCycle.getId());

      assertThat(decisions).hasSize(2);
    }

    @Test
    @DisplayName("Should find only committed decisions for a cycle")
    void findCommittedByCycleId_ShouldReturnOnlyCommitted() {
      // Add rejected decision
      BettingDecision rejectedDecision = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Rejected")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(21)
          .build();
      bettingDecisionRepository.save(rejectedDecision);

      List<BettingDecision> committed = bettingDecisionRepository
          .findCommittedByCycleId(testCycle.getId());

      assertThat(committed).hasSize(1);
      assertThat(committed.get(0).getDecision()).isEqualTo(BettingDecisionType.COMMITTED);
    }

    @Test
    @DisplayName("Should find only rejected decisions for a cycle")
    void findRejectedByCycleId_ShouldReturnOnlyRejected() {
      // Add rejected decision
      BettingDecision rejectedDecision = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Rejected for capacity reasons")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(21)
          .build();
      bettingDecisionRepository.save(rejectedDecision);

      List<BettingDecision> rejected = bettingDecisionRepository
          .findRejectedByCycleId(testCycle.getId());

      assertThat(rejected).hasSize(1);
      assertThat(rejected.get(0).getReason()).isEqualTo("Rejected for capacity reasons");
    }

    @Test
    @DisplayName("Should count decisions grouped by type")
    void countDecisionsByCycleIdGroupByType_ShouldReturnCounts() {
      // Add more decisions
      BettingDecision rejectedDecision = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Rejected")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(21)
          .build();
      bettingDecisionRepository.save(rejectedDecision);

      List<Object[]> counts = bettingDecisionRepository
          .countDecisionsByCycleIdGroupByType(testCycle.getId());

      assertThat(counts).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Appetite and Capacity Analysis")
  class AppetiteAnalysisTests {

    @Test
    @DisplayName("Should find decisions where appetite exceeded capacity")
    void findOverCapacityDecisions_ShouldReturnOverCapacity() {
      // Add over-capacity decision
      BettingDecision overCapacity = BettingDecision.builder()
          .pitch(testPitch2)
          .cycle(testCycle)
          .decision(BettingDecisionType.REJECTED)
          .reason("Over capacity")
          .decidedBy(testUser)
          .decidedAt(LocalDateTime.now())
          .requestedAppetiteDays(30)
          .availableCapacityDays(21)
          .build();
      bettingDecisionRepository.save(overCapacity);

      List<BettingDecision> overCapacityDecisions = bettingDecisionRepository
          .findOverCapacityDecisions();

      assertThat(overCapacityDecisions).hasSize(1);
      assertThat(overCapacityDecisions.get(0).getRequestedAppetiteDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Entity should correctly determine if appetite fits capacity")
    void appetiteFitsCapacity_ShouldReturnCorrectResult() {
      assertThat(testDecision.appetiteFitsCapacity()).isTrue();

      BettingDecision overCapacity = BettingDecision.builder()
          .requestedAppetiteDays(30)
          .availableCapacityDays(21)
          .build();
      assertThat(overCapacity.appetiteFitsCapacity()).isFalse();
    }
  }

  @Nested
  @DisplayName("Historical Analysis")
  class HistoricalAnalysisTests {

    @Test
    @DisplayName("Should find repeatedly rejected pitches")
    void findRepeatedlyRejectedPitches_ShouldReturnPitchesWithMultipleRejections() {
      // Reject same pitch multiple times
      for (int i = 0; i < 3; i++) {
        Cycle cycle = Cycle.builder()
            .name("Cycle " + i)
            .startDate(LocalDate.now().plusWeeks(i * 8))
            .endDate(LocalDate.now().plusWeeks((i + 1) * 8))
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(false)
            .build();
        cycle = cycleRepository.save(cycle);

        BettingDecision rejection = BettingDecision.builder()
            .pitch(testPitch2)
            .cycle(cycle)
            .decision(BettingDecisionType.REJECTED)
            .reason("Rejected again")
            .decidedBy(testUser)
            .decidedAt(LocalDateTime.now().plusDays(i))
            .requestedAppetiteDays(21)
            .build();
        bettingDecisionRepository.save(rejection);
      }

      List<Object[]> repeatedlyRejected = bettingDecisionRepository
          .findRepeatedlyRejectedPitches(3);

      assertThat(repeatedlyRejected).hasSize(1);
      assertThat(repeatedlyRejected.get(0)[0]).isEqualTo(testPitch2.getId());
    }

    @Test
    @DisplayName("Should find decisions within date range")
    void findByDecidedAtBetween_ShouldReturnDecisionsInRange() {
      LocalDateTime startDate = LocalDateTime.now().minusDays(1);
      LocalDateTime endDate = LocalDateTime.now().plusDays(1);

      List<BettingDecision> decisions = bettingDecisionRepository
          .findByDecidedAtBetween(startDate, endDate);

      assertThat(decisions).hasSize(1);
    }

    @Test
    @DisplayName("Should check if decision exists for pitch in cycle")
    void existsByPitchIdAndCycleId_ShouldReturnCorrectResult() {
      assertThat(bettingDecisionRepository.existsByPitchIdAndCycleId(
          testPitch.getId(), testCycle.getId())).isTrue();

      assertThat(bettingDecisionRepository.existsByPitchIdAndCycleId(
          testPitch2.getId(), testCycle.getId())).isFalse();
    }
  }

  @Nested
  @DisplayName("Team-based Queries")
  class TeamQueryTests {

    @Test
    @DisplayName("Should find decisions by considered team")
    void findByConsideredTeamIdOrderByDecidedAtDesc_ShouldReturnTeamDecisions() {
      List<BettingDecision> teamDecisions = bettingDecisionRepository
          .findByConsideredTeamIdOrderByDecidedAtDesc(testTeam.getId());

      assertThat(teamDecisions).hasSize(1);
      assertThat(teamDecisions.get(0).getConsideredTeam().getName()).isEqualTo("Test Team Alpha");
    }
  }

  @Nested
  @DisplayName("Entity Helper Methods")
  class EntityHelperTests {

    @Test
    @DisplayName("isCommitted should return true for COMMITTED decisions")
    void isCommitted_ShouldReturnCorrectResult() {
      assertThat(testDecision.isCommitted()).isTrue();

      BettingDecision rejected = BettingDecision.builder()
          .decision(BettingDecisionType.REJECTED)
          .build();
      assertThat(rejected.isCommitted()).isFalse();
    }
  }
}
