package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
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
class CycleRepositoryTest {

  @Autowired
  private CycleRepository cycleRepository;

  private Cycle testCycle;

  @BeforeEach
  void setUp() {
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.BUILD).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);
  }

  @Test
  void findAllByOrderByStartDateDesc_ShouldReturnCyclesOrdered() {
    Cycle olderCycle = Cycle.builder().name("Older Cycle").startDate(LocalDate.now().minusWeeks(8))
        .endDate(LocalDate.now().minusWeeks(2)).phase(CyclePhase.COOLDOWN).isActive(false).build();
    cycleRepository.save(olderCycle);

    List<Cycle> cycles = cycleRepository.findAllByOrderByStartDateDesc();

    assertThat(cycles).hasSize(2);
    assertThat(cycles.get(0).getName()).isEqualTo("Test Cycle");
  }

  @Test
  void findByIsActiveTrue_ShouldReturnActiveCycles() {
    Cycle inactiveCycle = Cycle.builder().name("Inactive Cycle").startDate(LocalDate.now().minusWeeks(8))
        .endDate(LocalDate.now().minusWeeks(2)).phase(CyclePhase.COOLDOWN).isActive(false).build();
    cycleRepository.save(inactiveCycle);

    List<Cycle> activeCycles = cycleRepository.findByIsActiveTrue();

    assertThat(activeCycles).hasSize(1);
    assertThat(activeCycles.get(0).getName()).isEqualTo("Test Cycle");
  }

  @Test
  void save_ShouldPersistCycle() {
    Cycle newCycle = Cycle.builder().name("New Cycle").startDate(LocalDate.now().plusMonths(1))
        .endDate(LocalDate.now().plusMonths(2)).phase(CyclePhase.BUILD).isActive(false).build();

    Cycle saved = cycleRepository.save(newCycle);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("New Cycle");
  }

  @Test
  void findById_WhenExists_ShouldReturnCycle() {
    var found = cycleRepository.findById(testCycle.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Cycle");
  }

  @Test
  void deleteById_ShouldRemoveCycle() {
    cycleRepository.deleteById(testCycle.getId());

    var found = cycleRepository.findById(testCycle.getId());
    assertThat(found).isEmpty();
  }
}
