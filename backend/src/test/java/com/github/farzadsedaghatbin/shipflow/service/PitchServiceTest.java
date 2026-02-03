package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PitchServiceTest {

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private AICacheService cacheService;

  @InjectMocks
  private PitchService pitchService;

  private Pitch testPitch;
  private Cycle testCycle;
  private Team testTeam;
  private CreatePitchRequest testRequest;

  @BeforeEach
  void setUp() {
    testCycle = Cycle.builder().id(1L).name("Test Cycle").build();

    testTeam = Team.builder().id(1L).name("Test Team").build();

    testPitch = Pitch.builder().id(1L).title("Test Pitch").description("Test Description").appetiteDays(14)
        .cycle(testCycle).team(testTeam).status(PitchStatus.PENDING).build();

    testRequest = new CreatePitchRequest();
    testRequest.setTitle("Test Pitch");
    testRequest.setDescription("Test Description");
    testRequest.setAppetiteDays(14);
    testRequest.setCycleId(1L);
    testRequest.setTeamId(1L);
    testRequest.setStatus(PitchStatus.PENDING);
  }

  @Test
  void getAllPitches_ShouldReturnAllPitches() {
    when(pitchRepository.findAll()).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getAllPitches();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void getPitchById_WhenExists_ShouldReturnPitch() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));

    PitchDTO result = pitchService.getPitchById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void getPitchById_WhenNotExists_ShouldThrowException() {
    when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> pitchService.getPitchById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Pitch not found");
  }

  @Test
  void createPitch_ShouldSavePitch() {
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    PitchDTO result = pitchService.createPitch(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Pitch");
    verify(pitchRepository).save(any(Pitch.class));
  }

  @Test
  void updatePitch_WhenExists_ShouldUpdatePitch() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    testRequest.setTitle("Updated Pitch");
    PitchDTO result = pitchService.updatePitch(1L, testRequest);

    assertThat(result).isNotNull();
    verify(pitchRepository).save(any(Pitch.class));
  }

  @Test
  void deletePitch_ShouldCallRepository() {
    doNothing().when(pitchRepository).deleteById(1L);

    pitchService.deletePitch(1L);

    verify(pitchRepository).deleteById(1L);
  }

  @Test
  void getPitchesByCycleId_ShouldReturnPitches() {
    when(pitchRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getPitchesByCycleId(1L);

    assertThat(result).hasSize(1);
    verify(pitchRepository).findByCycleId(1L);
  }

  @Test
  void getPitchesByTeamId_ShouldReturnPitches() {
    when(pitchRepository.findByTeamId(1L)).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getPitchesByTeamId(1L);

    assertThat(result).hasSize(1);
    verify(pitchRepository).findByTeamId(1L);
  }

  @Test
  void updateStatus_ShouldUpdatePitchStatus() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    PitchDTO result = pitchService.updateStatus(1L, PitchStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    verify(pitchRepository).save(any(Pitch.class));
  }
}
