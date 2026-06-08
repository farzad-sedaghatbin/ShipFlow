package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TeamAssignmentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TeamDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

  private final TeamRepository teamRepository;
  private final CapacityConfigService capacityConfigService;

  @Cacheable(value = "teams", key = "'all'")
  public List<TeamDTO> getAllTeams() {
    return teamRepository.findActiveWithAssignments().stream().map(this::toDTO).collect(Collectors.toList());
  }

  @Cacheable(value = "teams", key = "'allIncludingArchived'")
  public List<TeamDTO> getAllTeamsIncludingArchived() {
    return teamRepository.findAllWithAssignments().stream().map(this::toDTO).collect(Collectors.toList());
  }

  @Cacheable(value = "teams", key = "'byCycle:' + #cycleId")
  public List<TeamDTO> getTeamsByCycleId(Long cycleId) {
    return teamRepository.findByCycleId(cycleId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  @Cacheable(value = "teams", key = "'detail:' + #id")
  public TeamDTO getTeamById(Long id) {
    Team team = teamRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));
    return toDTO(team);
  }

  @CacheEvict(value = "teams", allEntries = true)
  public TeamDTO createTeam(CreateTeamRequest request) {
    Team team = Team.builder().name(request.getName()).build();

    // Set capacity overrides if provided
    if (request.getHoursPerDayOverride() != null) {
      if (request.getHoursPerDayOverride() < 1 || request.getHoursPerDayOverride() > 24) {
        throw new IllegalArgumentException("Hours per day must be between 1 and 24");
      }
      team.setHoursPerDayOverride(request.getHoursPerDayOverride());
    }
    if (request.getWorkingDaysPerWeekOverride() != null) {
      if (request.getWorkingDaysPerWeekOverride() < 1 || request.getWorkingDaysPerWeekOverride() > 7) {
        throw new IllegalArgumentException("Working days per week must be between 1 and 7");
      }
      team.setWorkingDaysPerWeekOverride(request.getWorkingDaysPerWeekOverride());
    }

    Team saved = teamRepository.save(team);
    return toDTO(saved);
  }

  @CacheEvict(value = "teams", allEntries = true)
  public TeamDTO updateTeam(Long id, CreateTeamRequest request) {
    Team team = teamRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));

    team.setName(request.getName());

    // Update capacity overrides
    if (request.getHoursPerDayOverride() != null) {
      if (request.getHoursPerDayOverride() < 1 || request.getHoursPerDayOverride() > 24) {
        throw new IllegalArgumentException("Hours per day must be between 1 and 24");
      }
      team.setHoursPerDayOverride(request.getHoursPerDayOverride());
    }
    if (request.getWorkingDaysPerWeekOverride() != null) {
      if (request.getWorkingDaysPerWeekOverride() < 1 || request.getWorkingDaysPerWeekOverride() > 7) {
        throw new IllegalArgumentException("Working days per week must be between 1 and 7");
      }
      team.setWorkingDaysPerWeekOverride(request.getWorkingDaysPerWeekOverride());
    }

    Team saved = teamRepository.save(team);
    return toDTO(saved);
  }

  @CacheEvict(value = "teams", allEntries = true)
  public void deleteTeam(Long id) {
    teamRepository.deleteById(id);
  }

  @CacheEvict(value = "teams", allEntries = true)
  public void archiveTeam(Long id) {
    Team team = teamRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));
    team.setIsArchived(true);
    teamRepository.save(team);
  }

  @CacheEvict(value = "teams", allEntries = true)
  public void unarchiveTeam(Long id) {
    Team team = teamRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));
    team.setIsArchived(false);
    teamRepository.save(team);
  }

  private TeamDTO toDTO(Team team) {
    List<TeamAssignmentDTO> assignmentDTOs = team.getAssignments() != null
        ? team.getAssignments().stream().filter(TeamAssignment::getIsActive).map(this::toAssignmentDTO)
            .collect(Collectors.toList())
        : List.of();

    // Resolve effective capacity values
    CapacityConfigService.CapacityResolution hoursResolution = 
        team.getHoursPerDayOverride() != null 
            ? new CapacityConfigService.CapacityResolution(team.getHoursPerDayOverride(), "team")
            : new CapacityConfigService.CapacityResolution(
                capacityConfigService.getOrganizationDefaultHoursPerDay(), "organization");
    
    CapacityConfigService.CapacityResolution daysResolution = 
        capacityConfigService.getEffectiveWorkingDaysPerWeek(team);

    // Calculate team capacity summary
    double totalDailyCapacity = team.getAssignments() != null 
        ? team.getAssignments().stream()
            .filter(a -> a.getIsActive() != null && a.getIsActive())
            .mapToDouble(a -> capacityConfigService.getEffectiveHoursPerDay(a).value())
            .sum()
        : 0;

    // Determine capacity source for the team
    String capacitySource = determineCapacitySource(team);

    TeamDTO.TeamCapacitySummary capacitySummary = TeamDTO.TeamCapacitySummary.builder()
        .memberCount(assignmentDTOs.size())
        .totalDailyCapacityHours(totalDailyCapacity)
        .capacitySource(capacitySource)
        .build();

    return TeamDTO.builder().id(team.getId()).name(team.getName())
        .isArchived(team.getIsArchived())
        .hoursPerDayOverride(team.getHoursPerDayOverride())
        .workingDaysPerWeekOverride(team.getWorkingDaysPerWeekOverride())
        .effectiveHoursPerDay(hoursResolution.value())
        .effectiveWorkingDaysPerWeek((int) daysResolution.value())
        .capacitySummary(capacitySummary)
        .assignments(assignmentDTOs).build();
  }

  private String determineCapacitySource(Team team) {
    if (team.getAssignments() == null || team.getAssignments().isEmpty()) {
      return team.getHoursPerDayOverride() != null ? "team" : "organization";
    }

    List<String> sources = team.getAssignments().stream()
        .filter(a -> a.getIsActive() != null && a.getIsActive())
        .map(a -> capacityConfigService.getEffectiveHoursPerDay(a).source())
        .distinct()
        .toList();

    if (sources.size() == 1) {
      return sources.get(0);
    }
    return "mixed";
  }

  private TeamAssignmentDTO toAssignmentDTO(TeamAssignment assignment) {
    // Resolve effective capacity
    CapacityConfigService.CapacityResolution capacityResolution = 
        capacityConfigService.getEffectiveHoursPerDay(assignment);

    return TeamAssignmentDTO.builder().id(assignment.getId()).personId(assignment.getPerson().getId())
        .personName(assignment.getPerson().getName()).teamId(assignment.getTeam().getId())
        .teamName(assignment.getTeam().getName()).role(assignment.getRole())
        .startDate(assignment.getStartDate()).endDate(assignment.getEndDate())
        .isActive(assignment.getIsActive())
        .hoursPerDayOverride(assignment.getHoursPerDayOverride())
        .effectiveHoursPerDay(capacityResolution.value())
        .capacitySource(capacityResolution.source())
        .build();
  }
}
