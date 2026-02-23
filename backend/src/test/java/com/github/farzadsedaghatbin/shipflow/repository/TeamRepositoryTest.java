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

@DataJpaTest
@ActiveProfiles("test")
class TeamRepositoryTest {

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  private Cycle testCycle;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testTeam = Team.builder().name("Test Team").build();
    testTeam = teamRepository.save(testTeam);

    // Link testTeam to testCycle via a pitch (required by the new findByCycleId query)
    Pitch testPitch = Pitch.builder().title("Test Pitch").description("Test Description").appetiteDays(14)
        .cycle(testCycle).team(testTeam).status(PitchStatus.PENDING).build();
    pitchRepository.save(testPitch);
  }

  @Test
  void findByCycleId_ShouldReturnTeamsForCycle() {
    List<Team> teams = teamRepository.findByCycleId(testCycle.getId());

    assertThat(teams).hasSize(1);
    assertThat(teams.get(0).getName()).isEqualTo("Test Team");
  }

  @Test
  void save_ShouldPersistTeam() {
    Team newTeam = Team.builder().name("New Team").build();

    Team saved = teamRepository.save(newTeam);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("New Team");
  }

  @Test
  void findById_WhenExists_ShouldReturnTeam() {
    var found = teamRepository.findById(testTeam.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Team");
  }

  @Test
  void deleteById_ShouldRemoveTeam() {
    teamRepository.deleteById(testTeam.getId());

    var found = teamRepository.findById(testTeam.getId());
    assertThat(found).isEmpty();
  }

  @Test
  void findAll_ShouldReturnAllTeams() {
    Team anotherTeam = Team.builder().name("Another Team").build();
    teamRepository.save(anotherTeam);

    List<Team> teams = teamRepository.findAll();

    assertThat(teams).hasSize(2);
  }
}
