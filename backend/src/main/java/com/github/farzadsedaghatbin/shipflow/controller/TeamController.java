package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TeamDTO;
import com.github.farzadsedaghatbin.shipflow.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management")
public class TeamController {

  private final TeamService teamService;

  @GetMapping
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'READ')")
  @Operation(summary = "Get all teams")
  public ResponseEntity<List<TeamDTO>> getAllTeams() {
    return ResponseEntity.ok(teamService.getAllTeams());
  }

  @GetMapping("/cycle/{cycleId}")
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'READ')")
  @Operation(summary = "Get teams by cycle ID")
  public ResponseEntity<List<TeamDTO>> getTeamsByCycleId(@PathVariable Long cycleId) {
    return ResponseEntity.ok(teamService.getTeamsByCycleId(cycleId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'READ')")
  @Operation(summary = "Get team by ID")
  public ResponseEntity<TeamDTO> getTeamById(@PathVariable Long id) {
    return ResponseEntity.ok(teamService.getTeamById(id));
  }

  @PostMapping
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'CREATE')")
  @Operation(summary = "Create a new team")
  public ResponseEntity<TeamDTO> createTeam(@Valid @RequestBody CreateTeamRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'UPDATE')")
  @Operation(summary = "Update a team")
  public ResponseEntity<TeamDTO> updateTeam(
      @PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {
    return ResponseEntity.ok(teamService.updateTeam(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissionService.hasPermission('TEAM', 'DELETE')")
  @Operation(summary = "Delete a team")
  public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
    teamService.deleteTeam(id);
    return ResponseEntity.noContent().build();
  }
}
