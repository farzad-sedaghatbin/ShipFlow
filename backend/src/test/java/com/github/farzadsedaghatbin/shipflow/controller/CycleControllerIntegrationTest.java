package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
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
class CycleControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  private Cycle testCycle;
  private Project testProject;

  @BeforeEach
  void setUp() {
    cycleRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();

    // Create test users matching @WithMockUser annotations
    User admin = User.builder().username("admin").email("admin@test.com").password("password").role(UserRole.ADMIN)
        .isActive(true).build();
    userRepository.save(admin);

    User developer = User.builder().username("developer").email("developer@test.com").password("password")
        .role(UserRole.MEMBER).isActive(true).build();
    userRepository.save(developer);

    User pm = User.builder().username("pm").email("pm@test.com").password("password").role(UserRole.MANAGER)
        .isActive(true).build();
    userRepository.save(pm);

    testProject = Project.builder().name("Test Project").projectKey("TST").isActive(true).build();
    testProject = projectRepository.save(testProject);

    testCycle = Cycle.builder().name("Test Cycle").project(testProject).phase(CyclePhase.BUILD)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);
  }

  @Test
  void getAllCycles_ShouldReturnCycles() throws Exception {
    mockMvc.perform(get("/api/cycles")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].name", is("Test Cycle")));
  }

  @Test
  void getActiveCycles_ShouldReturnActiveCycles() throws Exception {
    mockMvc.perform(get("/api/cycles/active")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void getCycleById_WhenExists_ShouldReturnCycle() throws Exception {
    mockMvc.perform(get("/api/cycles/{id}", testCycle.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testCycle.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Test Cycle")))
        .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())));
  }

  @Test
  void getCycleById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/cycles/{id}", 9999L)).andExpect(status().isNotFound());
  }

  @Test
  void createCycle_WithValidData_ShouldCreateCycle() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId()).name("New Cycle")
        .phase(CyclePhase.BUILD).startDate(LocalDate.now().plusMonths(1))
        // Don't set endDate - will be auto-calculated
        .build();

    mockMvc.perform(post("/api/cycles").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Cycle"))).andExpect(jsonPath("$.phase", is("BUILD")))
        .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())));
  }

  @Test
  void updateCycle_WhenExists_ShouldUpdateCycle() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId()).name("Updated Cycle")
        .phase(CyclePhase.COOLDOWN).startDate(LocalDate.now())
        // Don't set endDate - will be auto-calculated
        .build();

    mockMvc.perform(put("/api/cycles/{id}", testCycle.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Cycle"))).andExpect(jsonPath("$.phase", is("COOLDOWN")));
  }

  @Test
  void updatePhase_ShouldUpdateCyclePhase() throws Exception {
    mockMvc.perform(patch("/api/cycles/{id}/phase", testCycle.getId()).param("phase", "COOLDOWN"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.phase", is("COOLDOWN")));
  }

  @Test
  void deleteCycle_WhenExists_ShouldDeleteCycle() throws Exception {
    mockMvc.perform(delete("/api/cycles/{id}", testCycle.getId())).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/cycles/{id}", testCycle.getId())).andExpect(status().isNotFound());
  }

  @Test
  void getCyclesByProject_ShouldReturnCyclesForProject() throws Exception {
    mockMvc.perform(get("/api/cycles/project/{projectId}", testProject.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].projectId", is(testProject.getId().intValue())));
  }

  // ========== Auto-Calculation Feature Integration Tests ==========

  @Test
  @WithMockUser(username = "developer", roles = {"DEVELOPER"})
  void createCycle_WithoutEndDate_AsDeveloper_ShouldAutoCalculate() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId())
        .name("Auto-Calculated Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.of(2026, 2, 1))
        // Don't call .endDate() at all - let it be null by default
        .build();

    mockMvc.perform(post("/api/cycles").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Auto-Calculated Cycle")))
        .andExpect(jsonPath("$.startDate", is("2026-02-01"))).andExpect(jsonPath("$.endDate", notNullValue())); // End
    // date
    // should
    // be
    // calculated
  }

  @Test
  @WithMockUser(username = "developer", roles = {"DEVELOPER"})
  void createCycle_WithCustomEndDate_AsDeveloper_ShouldReturn403() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId())
        .name("Custom Length Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.of(2026, 2, 1))
        .endDate(LocalDate.of(2026, 2, 15)) // Only 2 weeks - should be rejected
        .build();

    mockMvc.perform(post("/api/cycles").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createCycle_WithCustomEndDate_AsAdmin_ShouldSucceed() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId())
        .name("Custom 4-Week Cycle").phase(CyclePhase.BUILD).startDate(LocalDate.of(2026, 2, 1))
        .endDate(LocalDate.of(2026, 3, 1)) // Custom 4-week cycle
        .build();

    mockMvc.perform(post("/api/cycles").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Custom 4-Week Cycle")))
        .andExpect(jsonPath("$.startDate", is("2026-02-01")))
        .andExpect(jsonPath("$.endDate", is("2026-03-01")));
  }

  @Test
  @WithMockUser(username = "pm", roles = {"PROJECT_MANAGER"})
  void createCycle_WithCustomEndDate_AsProjectManager_ShouldSucceed() throws Exception {
    CreateCycleRequest request = CreateCycleRequest.builder().projectId(testProject.getId()).name("PM Custom Cycle")
        .phase(CyclePhase.BUILD).startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 4, 26)) // 8-week
        // cycle
        .build();

    mockMvc.perform(post("/api/cycles").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("PM Custom Cycle")))
        .andExpect(jsonPath("$.endDate", is("2026-04-26")));
  }
}
