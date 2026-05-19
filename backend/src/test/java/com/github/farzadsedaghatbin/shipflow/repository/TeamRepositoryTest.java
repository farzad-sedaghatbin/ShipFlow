package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
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

  private Cycle testCycle;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build();

    testTeam = Team.builder().name("Test Team").build();

    // Assign team to cycle via the explicit join table
    testCycle.getTeams().add(testTeam);
    testCycle = cycleRepository.save(testCycle);
    testTeam = testCycle.getTeams().get(0);
  }

  @Test
  void findByCycleId_ShouldReturnTeamsAssignedToTheCycle() {
    List<Team> teams = teamRepository.findByCycleId(testCycle.getId());

    assertThat(teams).hasSize(1);
    assertThat(teams.get(0).getName()).isEqualTo("Test Team");
  }

  @Test
  void findByCycleId_WhenNoTeamsAssigned_ShouldReturnEmpty() {
    Cycle emptyCycle = cycleRepository.save(
        Cycle.builder().name("Empty Cycle").startDate(LocalDate.now())
            .endDate(LocalDate.now().plusWeeks(6))
            .phase(CyclePhase.SHAPING_BUILDING).isActive(true).build());

    List<Team> teams = teamRepository.findByCycleId(emptyCycle.getId());

    assertThat(teams).isEmpty();
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
    Team anotherTeam = teamRepository.save(Team.builder().name("Another Team").build());

    List<Team> teams = teamRepository.findAll();

    assertThat(teams).hasSize(2);
  }
}
