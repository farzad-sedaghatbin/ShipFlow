package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TeamDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private CapacityConfigService capacityConfigService;

  @InjectMocks
  private TeamService teamService;

  private Team testTeam;
  private CreateTeamRequest testRequest;

  @BeforeEach
  void setUp() {
    testTeam = Team.builder().id(1L).name("Test Team").assignments(new ArrayList<>()).build();

    testRequest = new CreateTeamRequest();
    testRequest.setName("Test Team");

    // Setup capacity config mock - default 8 hours/day
    lenient().when(capacityConfigService.getOrganizationDefaultHoursPerDay()).thenReturn(8.0);
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any())).thenReturn(80.0);
    lenient().when(capacityConfigService.getEffectiveWorkingDaysPerWeek(any(Team.class)))
        .thenReturn(new CapacityConfigService.CapacityResolution(5.0, "organization"));
    lenient().when(capacityConfigService.getEffectiveHoursPerDay(any(TeamAssignment.class)))
        .thenReturn(new CapacityConfigService.CapacityResolution(8.0, "organization"));
  }

  @Test
  void getAllTeams_ShouldReturnAllTeams() {
    when(teamRepository.findActiveWithAssignments()).thenReturn(Arrays.asList(testTeam));

    List<TeamDTO> result = teamService.getAllTeams();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Test Team");
  }

  @Test
  void getTeamById_WhenExists_ShouldReturnTeam() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));

    TeamDTO result = teamService.getTeamById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Team");
  }

  @Test
  void getTeamById_WhenNotExists_ShouldThrowException() {
    when(teamRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> teamService.getTeamById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Team not found");
  }

  @Test
  void createTeam_ShouldSaveTeam() {
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    TeamDTO result = teamService.createTeam(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Team");
    verify(teamRepository).save(any(Team.class));
  }

  @Test
  void updateTeam_WhenExists_ShouldUpdateTeam() {
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

    testRequest.setName("Updated Team");
    TeamDTO result = teamService.updateTeam(1L, testRequest);

    assertThat(result).isNotNull();
    verify(teamRepository).save(any(Team.class));
  }

  @Test
  void deleteTeam_ShouldCallRepository() {
    doNothing().when(teamRepository).deleteById(1L);

    teamService.deleteTeam(1L);

    verify(teamRepository).deleteById(1L);
  }

  @Test
  void getTeamsByCycleId_ShouldReturnTeams() {
    when(teamRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testTeam));

    List<TeamDTO> result = teamService.getTeamsByCycleId(1L);

    assertThat(result).hasSize(1);
    verify(teamRepository).findByCycleId(1L);
  }
}
