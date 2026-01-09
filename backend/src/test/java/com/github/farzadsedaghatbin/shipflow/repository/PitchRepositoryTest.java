package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
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

        testPitch = Pitch.builder()
                .title("Test Pitch")
                .description("Test Description")
                .appetiteDays(14)
                .cycle(testCycle)
                .team(testTeam)
                .status(PitchStatus.PENDING)
                .build();
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
        Pitch newPitch = Pitch.builder()
                .title("New Pitch")
                .description("New Description")
                .appetiteDays(7)
                .cycle(testCycle)
                .status(PitchStatus.IN_PROGRESS)
                .build();

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
}
