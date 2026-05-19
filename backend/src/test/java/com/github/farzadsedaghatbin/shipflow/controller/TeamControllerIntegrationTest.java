package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class TeamControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Autowired
  private UserRepository userRepository;

  private Cycle testCycle;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    permissionRepository.deleteAll();
    userRepository.deleteAll();
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    // Create test user
    User testUser = User.builder().username("admin").password("password").email("admin@test.com")
        .role(UserRole.MEMBER).build();
    testUser = userRepository.save(testUser);

    // Create permissions for MEMBER role
    Permission teamRead = Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.TEAM)
        .permissionType(PermissionType.READ).build();
    Permission teamCreate = Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.TEAM)
        .permissionType(PermissionType.CREATE).build();
    Permission teamUpdate = Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.TEAM)
        .permissionType(PermissionType.UPDATE).build();
    Permission teamDelete = Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.TEAM)
        .permissionType(PermissionType.DELETE).build();

    permissionRepository.save(teamRead);
    permissionRepository.save(teamCreate);
    permissionRepository.save(teamUpdate);
    permissionRepository.save(teamDelete);

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testTeam = Team.builder().name("Test Team").build();
    testTeam = teamRepository.save(testTeam);

    // Link testTeam to testCycle via the cycle_teams join table
    testCycle.getTeams().add(testTeam);
    testCycle = cycleRepository.save(testCycle);
  }

  @Test
  void getAllTeams_ShouldReturnTeams() throws Exception {
    mockMvc.perform(get("/api/teams")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].name", is("Test Team")));
  }

  @Test
  void getTeamById_WhenExists_ShouldReturnTeam() throws Exception {
    mockMvc.perform(get("/api/teams/{id}", testTeam.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testTeam.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Test Team")));
  }

  @Test
  void getTeamById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/teams/{id}", 9999L)).andExpect(status().isBadRequest());
  }

  @Test
  void createTeam_WithValidData_ShouldCreateTeam() throws Exception {
    CreateTeamRequest request = CreateTeamRequest.builder().name("New Team").build();

    mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Team")));
  }

  @Test
  void updateTeam_WhenExists_ShouldUpdateTeam() throws Exception {
    CreateTeamRequest request = CreateTeamRequest.builder().name("Updated Team").build();

    mockMvc.perform(put("/api/teams/{id}", testTeam.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Team")));
  }

  @Test
  void deleteTeam_WhenExists_ShouldDeleteTeam() throws Exception {
    mockMvc.perform(delete("/api/teams/{id}", testTeam.getId())).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/teams/{id}", testTeam.getId())).andExpect(status().isBadRequest());
  }

  @Test
  void getTeamsByCycle_ShouldReturnTeamsForCycle() throws Exception {
    mockMvc.perform(get("/api/teams/cycle/{cycleId}", testCycle.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Test Team")));
  }
}
