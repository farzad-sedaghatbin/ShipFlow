package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateReleaseRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ReleaseControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ReleaseRepository releaseRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  private Project testProject;
  private Release testRelease;
  private Cycle testCycle;

  @BeforeEach
  void setUp() {
    cycleRepository.deleteAll();
    releaseRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();
    projectRepository.flush();

    // Create admin user
    Person adminPerson = Person.builder()
        .name("Admin User")
        .email("admin@example.com")
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    adminPerson = personRepository.save(adminPerson);

    User adminUser = User.builder()
        .username("user")
        .password("password")
        .role(UserRole.ADMIN)
        .person(adminPerson)
        .isActive(true)
        .build();
    userRepository.save(adminUser);

    testProject = Project.builder()
        .name("Test Project")
        .projectKey("TST")
        .description("Test Description")
        .isActive(true)
        .build();
    testProject = projectRepository.save(testProject);

    testCycle = Cycle.builder()
        .name("Q1 Cycle")
        .project(testProject)
        .startDate(LocalDate.of(2025, 1, 1))
        .endDate(LocalDate.of(2025, 2, 28))
        .phase(CyclePhase.BUILD)
        .isActive(true)
        .build();
    testCycle = cycleRepository.save(testCycle);

    testRelease = Release.builder()
        .name("Release 1.0")
        .version("1.0.0")
        .description("First major release")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.LOW)
        .project(testProject)
        .targetDate(LocalDate.of(2025, 3, 1))
        .createdAt(LocalDateTime.now())
        .build();
    testRelease = releaseRepository.save(testRelease);
    releaseRepository.flush();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getReleasesByProject_ShouldReturnReleases() throws Exception {
    mockMvc.perform(get("/api/releases/project/{projectId}", testProject.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Release 1.0")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getUpcomingByProject_ShouldReturnUpcomingReleases() throws Exception {
    // Create a release with a future target date
    Release futureRelease = Release.builder()
        .name("Future Release")
        .version("2.0.0")
        .description("A future release")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.LOW)
        .project(testProject)
        .targetDate(LocalDate.now().plusMonths(3))
        .createdAt(LocalDateTime.now())
        .build();
    releaseRepository.save(futureRelease);

    mockMvc.perform(get("/api/releases/project/{projectId}/upcoming", testProject.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getReleaseById_WhenExists_ShouldReturnRelease() throws Exception {
    mockMvc.perform(get("/api/releases/{id}", testRelease.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testRelease.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Release 1.0")))
        .andExpect(jsonPath("$.version", is("1.0.0")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getReleaseById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/releases/{id}", 99999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createRelease_ShouldCreateAndReturnRelease() throws Exception {
    CreateReleaseRequest request = CreateReleaseRequest.builder()
        .name("Release 2.0")
        .version("2.0.0")
        .description("Second major release")
        .projectId(testProject.getId())
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.MEDIUM)
        .targetDate(LocalDate.of(2025, 6, 1))
        .build();

    mockMvc.perform(post("/api/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Release 2.0")))
        .andExpect(jsonPath("$.version", is("2.0.0")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createRelease_WithInvalidProject_ShouldReturn404() throws Exception {
    CreateReleaseRequest request = CreateReleaseRequest.builder()
        .name("New Release")
        .version("1.0.0")
        .projectId(99999L)
        .build();

    mockMvc.perform(post("/api/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRelease_ShouldUpdateAndReturnRelease() throws Exception {
    CreateReleaseRequest request = CreateReleaseRequest.builder()
        .name("Updated Release")
        .version("1.0.1")
        .description("Updated description")
        .projectId(testProject.getId())
        .status(ReleaseStatus.IN_PROGRESS)
        .riskLevel(ReleaseRiskLevel.HIGH)
        .build();

    mockMvc.perform(put("/api/releases/{id}", testRelease.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Release")))
        .andExpect(jsonPath("$.version", is("1.0.1")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateStatus_ShouldUpdateStatus() throws Exception {
    mockMvc.perform(patch("/api/releases/{id}/status", testRelease.getId())
            .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void linkCycle_ShouldLinkCycleToRelease() throws Exception {
    mockMvc.perform(patch("/api/releases/{id}/link-cycle", testRelease.getId())
            .param("cycleId", testCycle.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycles", hasSize(1)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteRelease_ShouldDelete() throws Exception {
    mockMvc.perform(delete("/api/releases/{id}", testRelease.getId()))
        .andExpect(status().isNoContent());

    // Verify deletion (soft delete)
    mockMvc.perform(get("/api/releases/{id}", testRelease.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getProgress_ShouldReturnProgress() throws Exception {
    mockMvc.perform(get("/api/releases/{id}/progress", testRelease.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.completionPercentage").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getReleasesByStatus_ShouldReturnFilteredReleases() throws Exception {
    mockMvc.perform(get("/api/releases/project/{projectId}/status/{status}", 
            testProject.getId(), "PLANNING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].status", is("PLANNING")));
  }
}
