package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
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
class CycleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CycleRepository cycleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Cycle testCycle;
    private Project testProject;

    @BeforeEach
    void setUp() {
        cycleRepository.deleteAll();
        projectRepository.deleteAll();

        testProject = Project.builder()
                .name("Test Project")
                .projectKey("TST")
                .isActive(true)
                .build();
        testProject = projectRepository.save(testProject);
        
        testCycle = Cycle.builder()
                .name("Test Cycle")
                .project(testProject)
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .isActive(true)
                .build();
        testCycle = cycleRepository.save(testCycle);
    }

    @Test
    void getAllCycles_ShouldReturnCycles() throws Exception {
        mockMvc.perform(get("/api/cycles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Test Cycle")));
    }

    @Test
    void getActiveCycles_ShouldReturnActiveCycles() throws Exception {
        mockMvc.perform(get("/api/cycles/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getCycleById_WhenExists_ShouldReturnCycle() throws Exception {
        mockMvc.perform(get("/api/cycles/{id}", testCycle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testCycle.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Cycle")))
                .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())));
    }

    @Test
    void getCycleById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/cycles/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCycle_WithValidData_ShouldCreateCycle() throws Exception {
        CreateCycleRequest request = CreateCycleRequest.builder()
                .projectId(testProject.getId())
                .name("New Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now().plusMonths(1))
                .endDate(LocalDate.now().plusMonths(2))
                .build();

        mockMvc.perform(post("/api/cycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Cycle")))
                .andExpect(jsonPath("$.phase", is("BUILD")))
                .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())));
    }

    @Test
    void updateCycle_WhenExists_ShouldUpdateCycle() throws Exception {
        CreateCycleRequest request = CreateCycleRequest.builder()
                .projectId(testProject.getId())
                .name("Updated Cycle")
                .phase(CyclePhase.COOLDOWN)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();

        mockMvc.perform(put("/api/cycles/{id}", testCycle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Cycle")))
                .andExpect(jsonPath("$.phase", is("COOLDOWN")));
    }

    @Test
    void updatePhase_ShouldUpdateCyclePhase() throws Exception {
        mockMvc.perform(patch("/api/cycles/{id}/phase", testCycle.getId())
                        .param("phase", "COOLDOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase", is("COOLDOWN")));
    }

    @Test
    void deleteCycle_WhenExists_ShouldDeleteCycle() throws Exception {
        mockMvc.perform(delete("/api/cycles/{id}", testCycle.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cycles/{id}", testCycle.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCyclesByProject_ShouldReturnCyclesForProject() throws Exception {
        mockMvc.perform(get("/api/cycles/project/{projectId}", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].projectId", is(testProject.getId().intValue())));
    }
}
