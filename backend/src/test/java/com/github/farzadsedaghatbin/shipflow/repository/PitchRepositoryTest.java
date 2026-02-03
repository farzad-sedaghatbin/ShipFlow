package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class PitchRepositoryTest {

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private TeamRepository teamRepository;

  private Cycle testCycle;
  private Team testTeam;
  private Pitch testPitch;

  @BeforeEach
  void setUp() {
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.BUILD).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testTeam = Team.builder().name("Test Team").cycle(testCycle).build();
    testTeam = teamRepository.save(testTeam);

    testPitch = Pitch.builder().title("Test Pitch").description("Test Description").appetiteDays(14)
        .cycle(testCycle).team(testTeam).status(PitchStatus.PENDING).build();
    testPitch = pitchRepository.save(testPitch);
  }

  @Test
  void findByCycleId_ShouldReturnPitchesForCycle() {
    List<Pitch> pitches = pitchRepository.findByCycleId(testCycle.getId());

    assertThat(pitches).hasSize(1);
    assertThat(pitches.get(0).getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void findByTeamId_ShouldReturnPitchesForTeam() {
    List<Pitch> pitches = pitchRepository.findByTeamId(testTeam.getId());

    assertThat(pitches).hasSize(1);
    assertThat(pitches.get(0).getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void save_ShouldPersistPitch() {
    Pitch newPitch = Pitch.builder().title("New Pitch").description("New Description").appetiteDays(7)
        .cycle(testCycle).status(PitchStatus.IN_PROGRESS).build();

    Pitch saved = pitchRepository.save(newPitch);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("New Pitch");
  }

  @Test
  void deleteById_ShouldRemovePitch() {
    pitchRepository.deleteById(testPitch.getId());

    var found = pitchRepository.findById(testPitch.getId());
    assertThat(found).isEmpty();
  }

  @Test
  void findByCycleIdAndStatusIn_ShouldReturnPitchesWithSpecifiedStatuses() {
    // Create pitches with different statuses
    Pitch inProgressPitch = Pitch.builder().title("In Progress Pitch").description("Test").appetiteDays(10)
        .cycle(testCycle).team(testTeam).status(PitchStatus.IN_PROGRESS).build();
    pitchRepository.save(inProgressPitch);

    Pitch startedPitch = Pitch.builder().title("Started Pitch").description("Test").appetiteDays(10)
        .cycle(testCycle).team(testTeam).status(PitchStatus.STARTED).build();
    pitchRepository.save(startedPitch);

    Pitch completedPitch = Pitch.builder().title("Completed Pitch").description("Test").appetiteDays(10)
        .cycle(testCycle).team(testTeam).status(PitchStatus.DONE).build();
    pitchRepository.save(completedPitch);

    // Act
    List<PitchStatus> statuses = List.of(PitchStatus.IN_PROGRESS, PitchStatus.STARTED);
    List<Pitch> result = pitchRepository.findByCycleIdAndStatusIn(testCycle.getId(), statuses);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(Pitch::getStatus).containsExactlyInAnyOrder(PitchStatus.IN_PROGRESS,
        PitchStatus.STARTED);
    assertThat(result).extracting(Pitch::getTitle).containsExactlyInAnyOrder("In Progress Pitch", "Started Pitch");
  }

  @Test
  void findByCycleIdAndStatusIn_ShouldReturnEmptyListWhenNoMatches() {
    // Act
    List<PitchStatus> statuses = List.of(PitchStatus.CIRCUIT_BREAKER, PitchStatus.CANCELLED);
    List<Pitch> result = pitchRepository.findByCycleIdAndStatusIn(testCycle.getId(), statuses);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void countByCycleIdNotDeleted_ShouldExcludeDeletedPitches() {
    // Arrange - Create multiple pitches, some deleted
    Pitch activePitch1 = Pitch.builder().title("Active Pitch 1").description("Description 1").appetiteDays(6)
        .cycle(testCycle).status(PitchStatus.PENDING).build();

    Pitch activePitch2 = Pitch.builder().title("Active Pitch 2").description("Description 2").appetiteDays(8)
        .cycle(testCycle).status(PitchStatus.IN_PROGRESS).build();

    Pitch deletedPitch = Pitch.builder().title("Deleted Pitch").description("Description 3").appetiteDays(4)
        .cycle(testCycle).status(PitchStatus.DONE).build();

    // Save all pitches
    pitchRepository.save(activePitch1);
    pitchRepository.save(activePitch2);
    pitchRepository.save(deletedPitch);

    // Soft delete one pitch
    deletedPitch.setDeletedAt(java.time.LocalDateTime.now());
    pitchRepository.save(deletedPitch);

    // Act
    long count = pitchRepository.countByCycleIdNotDeleted(testCycle.getId());

    // Assert - Should only count the 2 non-deleted pitches
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countByCycleIdNotDeleted_ShouldReturnZero_WhenNoPitchesExist() {
    // Act
    long count = pitchRepository.countByCycleIdNotDeleted(testCycle.getId());

    // Assert
    assertThat(count).isEqualTo(0);
  }

  @Test
  void countByCycleIdNotDeleted_ShouldReturnZero_WhenAllPitchesAreDeleted() {
    // Arrange - Create and delete all pitches
    Pitch pitch1 = Pitch.builder().title("Pitch 1").description("Description 1").appetiteDays(6).cycle(testCycle)
        .status(PitchStatus.PENDING).build();

    Pitch pitch2 = Pitch.builder().title("Pitch 2").description("Description 2").appetiteDays(8).cycle(testCycle)
        .status(PitchStatus.IN_PROGRESS).build();

    pitchRepository.save(pitch1);
    pitchRepository.save(pitch2);

    // Soft delete both pitches
    pitch1.setDeletedAt(java.time.LocalDateTime.now());
    pitch2.setDeletedAt(java.time.LocalDateTime.now());
    pitchRepository.save(pitch1);
    pitchRepository.save(pitch2);

    // Act
    long count = pitchRepository.countByCycleIdNotDeleted(testCycle.getId());

    // Assert
    assertThat(count).isEqualTo(0);
  }

  @Test
  void countByCycleIdNotDeleted_ShouldCountOnlyForSpecificCycle() {
    // Arrange - Create another cycle with pitches
    Cycle anotherCycle = Cycle.builder().name("Another Cycle").startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).phase(CyclePhase.BUILD).isActive(true).build();
    anotherCycle = cycleRepository.save(anotherCycle);

    // Create pitches for original cycle
    Pitch pitch1 = Pitch.builder().title("Pitch 1").description("Description 1").appetiteDays(6).cycle(testCycle)
        .status(PitchStatus.PENDING).build();

    // Create pitches for another cycle
    Pitch pitch2 = Pitch.builder().title("Pitch 2").description("Description 2").appetiteDays(8).cycle(anotherCycle)
        .status(PitchStatus.IN_PROGRESS).build();

    pitchRepository.save(pitch1);
    pitchRepository.save(pitch2);

    // Act
    long countForTestCycle = pitchRepository.countByCycleIdNotDeleted(testCycle.getId());
    long countForAnotherCycle = pitchRepository.countByCycleIdNotDeleted(anotherCycle.getId());

    // Assert
    assertThat(countForTestCycle).isEqualTo(1);
    assertThat(countForAnotherCycle).isEqualTo(1);
  }
}
