package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
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
class RoadmapControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private InitiativeRepository initiativeRepository;

  @Autowired
  private EpicRepository epicRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  private Project testProject;
  private Initiative testInitiative;
  private Epic testEpic;

  @BeforeEach
  void setUp() {
    epicRepository.deleteAll();
    initiativeRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();
    projectRepository.flush();

    // Create admin user to match @WithMockUser
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
        .color("#FF0000")
        .isActive(true)
        .build();
    testProject = projectRepository.save(testProject);

    testInitiative = Initiative.builder()
        .name("Q1 Feature Set")
        .description("Features for Q1")
        .status(InitiativeStatus.IN_PROGRESS)
        .project(testProject)
        .targetStartDate(LocalDate.of(2025, 1, 1))
        .targetEndDate(LocalDate.of(2025, 3, 31))
        .color("#6366f1")
        .createdAt(LocalDateTime.now())
        .build();
    testInitiative = initiativeRepository.save(testInitiative);

    testEpic = Epic.builder()
        .name("User Authentication")
        .description("Auth module")
        .status(EpicStatus.IN_PROGRESS)
        .project(testProject)
        .initiative(testInitiative)
        .targetStartDate(LocalDate.of(2025, 1, 15))
        .targetEndDate(LocalDate.of(2025, 2, 15))
        .color("#8b5cf6")
        .createdAt(LocalDateTime.now())
        .build();
    testEpic = epicRepository.save(testEpic);

    projectRepository.flush();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRoadmapTimeline_ShouldReturnTimeline() throws Exception {
    mockMvc.perform(get("/api/roadmap/project/{projectId}/timeline", testProject.getId())
            .param("startDate", "2025-01-01")
            .param("endDate", "2025-03-31"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())))
        .andExpect(jsonPath("$.initiatives", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.initiatives[0].name", is("Q1 Feature Set")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getQuarterlyRoadmap_ShouldReturnQuarterlyView() throws Exception {
    mockMvc.perform(get("/api/roadmap/project/{projectId}/quarterly", testProject.getId())
            .param("year", "2025")
            .param("quarter", "1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())))
        .andExpect(jsonPath("$.initiatives", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getYearlyRoadmap_ShouldReturnYearlyView() throws Exception {
    mockMvc.perform(get("/api/roadmap/project/{projectId}/yearly", testProject.getId())
            .param("year", "2025"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.projectId", is(testProject.getId().intValue())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRoadmapTimeline_WithNoData_ShouldReturnEmptyTimeline() throws Exception {
    Project emptyProject = Project.builder()
        .name("Empty Project")
        .projectKey("EMP")
        .isActive(true)
        .build();
    emptyProject = projectRepository.save(emptyProject);

    mockMvc.perform(get("/api/roadmap/project/{projectId}/timeline", emptyProject.getId())
            .param("startDate", "2025-01-01")
            .param("endDate", "2025-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.initiatives", hasSize(0)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getRoadmapTimeline_InvalidProject_ShouldReturnEmptyTimeline() throws Exception {
    mockMvc.perform(get("/api/roadmap/project/{projectId}/timeline", 99999L)
            .param("startDate", "2025-01-01")
            .param("endDate", "2025-03-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.initiatives", hasSize(0)));
  }
}
