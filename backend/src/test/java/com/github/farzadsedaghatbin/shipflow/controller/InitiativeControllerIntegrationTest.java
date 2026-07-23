package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateInitiativeRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
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
class InitiativeControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private InitiativeRepository initiativeRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  private Project testProject;
  private Initiative testInitiative;
  private Person adminPerson;
  private Person memberPerson;

  @BeforeEach
  void setUp() {
    initiativeRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();
    projectRepository.flush();

    // Create admin user
    adminPerson = Person.builder()
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

    // Create member user (for MEMBER create-initiative coverage)
    memberPerson = Person.builder()
        .name("Member User")
        .email("member@example.com")
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();
    memberPerson = personRepository.save(memberPerson);

    User memberUser = User.builder()
        .username("member")
        .password("password")
        .role(UserRole.MEMBER)
        .person(memberPerson)
        .isActive(true)
        .build();
    userRepository.save(memberUser);

    testProject = Project.builder()
        .name("Test Project")
        .projectKey("TST")
        .description("Test Description")
        .isActive(true)
        .build();
    testProject = projectRepository.save(testProject);

    testInitiative = Initiative.builder()
        .name("Test Initiative")
        .description("Test Description")
        .status(InitiativeStatus.PLANNED)
        .project(testProject)
        .targetStartDate(LocalDate.of(2025, 1, 1))
        .targetEndDate(LocalDate.of(2025, 6, 30))
        .color("#6366f1")
        .createdAt(LocalDateTime.now())
        .build();
    testInitiative = initiativeRepository.save(testInitiative);
    initiativeRepository.flush();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getInitiativesByProject_ShouldReturnInitiatives() throws Exception {
    mockMvc.perform(get("/api/initiatives/project/{projectId}", testProject.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Test Initiative")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getInitiativeById_WhenExists_ShouldReturnInitiative() throws Exception {
    mockMvc.perform(get("/api/initiatives/{id}", testInitiative.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testInitiative.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Test Initiative")))
        .andExpect(jsonPath("$.status", is("PLANNED")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getInitiativeById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/initiatives/{id}", 99999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createInitiative_ShouldCreateAndReturnInitiative() throws Exception {
    CreateInitiativeRequest request = CreateInitiativeRequest.builder()
        .name("New Initiative")
        .description("New Description")
        .projectId(testProject.getId())
        .status(InitiativeStatus.DRAFT)
        .targetStartDate(LocalDate.of(2025, 7, 1))
        .targetEndDate(LocalDate.of(2025, 12, 31))
        .color("#10b981")
        .build();

    mockMvc.perform(post("/api/initiatives")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Initiative")))
        .andExpect(jsonPath("$.status", is("DRAFT")));
  }

  @Test
  @WithMockUser(username = "member", roles = "MEMBER")
  void createInitiative_AsMember_ShouldCreateAndReturnInitiative() throws Exception {
    CreateInitiativeRequest request = CreateInitiativeRequest.builder()
        .name("Member Initiative")
        .description("Created by a member")
        .projectId(testProject.getId())
        .status(InitiativeStatus.DRAFT)
        .targetStartDate(LocalDate.of(2025, 7, 1))
        .targetEndDate(LocalDate.of(2025, 12, 31))
        .color("#10b981")
        .build();

    mockMvc.perform(post("/api/initiatives")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Member Initiative")));
  }

  @Test
  @WithMockUser(username = "member", roles = "MEMBER")
  void updateInitiative_AsMember_ShouldBeForbidden() throws Exception {
    CreateInitiativeRequest request = CreateInitiativeRequest.builder()
        .name("Should Not Update")
        .projectId(testProject.getId())
        .status(InitiativeStatus.IN_PROGRESS)
        .build();

    mockMvc.perform(put("/api/initiatives/{id}", testInitiative.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createInitiative_WithInvalidProject_ShouldReturn404() throws Exception {
    CreateInitiativeRequest request = CreateInitiativeRequest.builder()
        .name("New Initiative")
        .projectId(99999L)
        .build();

    mockMvc.perform(post("/api/initiatives")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateInitiative_ShouldUpdateAndReturnInitiative() throws Exception {
    CreateInitiativeRequest request = CreateInitiativeRequest.builder()
        .name("Updated Initiative")
        .description("Updated Description")
        .projectId(testProject.getId())
        .status(InitiativeStatus.IN_PROGRESS)
        .build();

    mockMvc.perform(put("/api/initiatives/{id}", testInitiative.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Initiative")))
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateStatus_ShouldUpdateStatus() throws Exception {
    mockMvc.perform(patch("/api/initiatives/{id}/status", testInitiative.getId())
            .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteInitiative_ShouldDelete() throws Exception {
    mockMvc.perform(delete("/api/initiatives/{id}", testInitiative.getId()))
        .andExpect(status().isNoContent());

    // Verify deletion (soft delete)
    mockMvc.perform(get("/api/initiatives/{id}", testInitiative.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getInitiativesByStatus_ShouldReturnFilteredInitiatives() throws Exception {
    mockMvc.perform(get("/api/initiatives/project/{projectId}/status/{status}", 
            testProject.getId(), "PLANNED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].status", is("PLANNED")));
  }
}
