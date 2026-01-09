package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

        testCycle = Cycle.builder()
                .name("Test Cycle")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .phase(CyclePhase.BUILD)
                .isActive(true)
                .build();
        testCycle = cycleRepository.save(testCycle);

        testTeam = Team.builder()
                .name("Test Team")
                .cycle(testCycle)
                .build();
        testTeam = teamRepository.save(testTeam);
    }

    @Test
    void findByCycleId_ShouldReturnTeamsForCycle() {
        List<Team> teams = teamRepository.findByCycleId(testCycle.getId());

        assertThat(teams).hasSize(1);
        assertThat(teams.get(0).getName()).isEqualTo("Test Team");
    }

    @Test
    void save_ShouldPersistTeam() {
        Team newTeam = Team.builder()
                .name("New Team")
                .cycle(testCycle)
                .build();

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
        Team anotherTeam = Team.builder()
                .name("Another Team")
                .cycle(testCycle)
                .build();
        teamRepository.save(anotherTeam);

        List<Team> teams = teamRepository.findAll();

        assertThat(teams).hasSize(2);
    }
}
