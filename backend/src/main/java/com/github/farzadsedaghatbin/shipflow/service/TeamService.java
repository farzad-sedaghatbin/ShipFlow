package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TeamAssignmentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TeamDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final CycleRepository cycleRepository;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TeamDTO> getTeamsByCycleId(Long cycleId) {
        return teamRepository.findByCycleId(cycleId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TeamDTO getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));
        return toDTO(team);
    }

    public TeamDTO createTeam(CreateTeamRequest request) {
        Team team = Team.builder()
                .name(request.getName())
                .build();
        
        if (request.getCycleId() != null) {
            Cycle cycle = cycleRepository.findById(request.getCycleId())
                    .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));
            team.setCycle(cycle);
        }
        
        Team saved = teamRepository.save(team);
        return toDTO(saved);
    }

    public TeamDTO updateTeam(Long id, CreateTeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + id));
        
        team.setName(request.getName());
        
        if (request.getCycleId() != null) {
            Cycle cycle = cycleRepository.findById(request.getCycleId())
                    .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));
            team.setCycle(cycle);
        }
        
        Team saved = teamRepository.save(team);
        return toDTO(saved);
    }

    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }

    private TeamDTO toDTO(Team team) {
        List<TeamAssignmentDTO> assignmentDTOs = team.getAssignments() != null
                ? team.getAssignments().stream()
                    .filter(TeamAssignment::getIsActive)
                    .map(this::toAssignmentDTO)
                    .collect(Collectors.toList())
                : List.of();
        
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .cycleId(team.getCycle() != null ? team.getCycle().getId() : null)
                .cycleName(team.getCycle() != null ? team.getCycle().getName() : null)
                .projectId(team.getCycle() != null && team.getCycle().getProject() != null ? team.getCycle().getProject().getId() : null)
                .projectName(team.getCycle() != null && team.getCycle().getProject() != null ? team.getCycle().getProject().getName() : null)
                .projectKey(team.getCycle() != null && team.getCycle().getProject() != null ? team.getCycle().getProject().getProjectKey() : null)
                .assignments(assignmentDTOs)
                .build();
    }

    private TeamAssignmentDTO toAssignmentDTO(TeamAssignment assignment) {
        return TeamAssignmentDTO.builder()
                .id(assignment.getId())
                .personId(assignment.getPerson().getId())
                .personName(assignment.getPerson().getName())
                .teamId(assignment.getTeam().getId())
                .teamName(assignment.getTeam().getName())
                .role(assignment.getRole())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .isActive(assignment.getIsActive())
                .build();
    }
}
