package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateTeamRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private Cycle testCycle;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        teamRepository.deleteAll();
        cycleRepository.deleteAll();
        
        testCycle = Cycle.builder()
                .name("Test Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
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
    void getAllTeams_ShouldReturnTeams() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Test Team")));
    }

    @Test
    void getTeamById_WhenExists_ShouldReturnTeam() throws Exception {
        mockMvc.perform(get("/api/teams/{id}", testTeam.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testTeam.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Team")));
    }

    @Test
    void getTeamById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/teams/{id}", 9999L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTeam_WithValidData_ShouldCreateTeam() throws Exception {
        CreateTeamRequest request = CreateTeamRequest.builder()
                .name("New Team")
                .cycleId(testCycle.getId())
                .build();

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Team")));
    }

    @Test
    void updateTeam_WhenExists_ShouldUpdateTeam() throws Exception {
        CreateTeamRequest request = CreateTeamRequest.builder()
                .name("Updated Team")
                .cycleId(testCycle.getId())
                .build();

        mockMvc.perform(put("/api/teams/{id}", testTeam.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Team")));
    }

    @Test
    void deleteTeam_WhenExists_ShouldDeleteTeam() throws Exception {
        mockMvc.perform(delete("/api/teams/{id}", testTeam.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teams/{id}", testTeam.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTeamsByCycle_ShouldReturnTeamsForCycle() throws Exception {
        mockMvc.perform(get("/api/teams/cycle/{cycleId}", testCycle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Team")));
    }
}
