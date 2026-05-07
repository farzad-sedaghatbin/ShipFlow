package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
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
class EpicControllerIntegrationTest {

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

    testInitiative = Initiative.builder()
        .name("Test Initiative")
        .description("Test Description")
        .status(InitiativeStatus.PLANNED)
        .project(testProject)
        .createdAt(LocalDateTime.now())
        .build();
    testInitiative = initiativeRepository.save(testInitiative);

    testEpic = Epic.builder()
        .name("Test Epic")
        .description("Test Description")
        .status(EpicStatus.PLANNED)
        .project(testProject)
        .initiative(testInitiative)
        .targetStartDate(LocalDate.of(2025, 1, 15))
        .targetEndDate(LocalDate.of(2025, 2, 15))
        .color("#8b5cf6")
        .createdAt(LocalDateTime.now())
        .build();
    testEpic = epicRepository.save(testEpic);
    epicRepository.flush();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEpicsByProject_ShouldReturnEpics() throws Exception {
    mockMvc.perform(get("/api/epics/project/{projectId}", testProject.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Test Epic")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEpicsByInitiative_ShouldReturnEpics() throws Exception {
    mockMvc.perform(get("/api/epics/initiative/{initiativeId}", testInitiative.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Test Epic")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEpicById_WhenExists_ShouldReturnEpic() throws Exception {
    mockMvc.perform(get("/api/epics/{id}", testEpic.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testEpic.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Test Epic")))
        .andExpect(jsonPath("$.status", is("PLANNED")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEpicById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/epics/{id}", 99999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createEpic_ShouldCreateAndReturnEpic() throws Exception {
    CreateEpicRequest request = CreateEpicRequest.builder()
        .name("New Epic")
        .description("New Description")
        .projectId(testProject.getId())
        .initiativeId(testInitiative.getId())
        .status(EpicStatus.DRAFT)
        .targetStartDate(LocalDate.of(2025, 3, 1))
        .targetEndDate(LocalDate.of(2025, 4, 30))
        .color("#10b981")
        .build();

    mockMvc.perform(post("/api/epics")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Epic")))
        .andExpect(jsonPath("$.status", is("DRAFT")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createEpic_WithInvalidProject_ShouldReturn404() throws Exception {
    CreateEpicRequest request = CreateEpicRequest.builder()
        .name("New Epic")
        .projectId(99999L)
        .build();

    mockMvc.perform(post("/api/epics")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateEpic_ShouldUpdateAndReturnEpic() throws Exception {
    CreateEpicRequest request = CreateEpicRequest.builder()
        .name("Updated Epic")
        .description("Updated Description")
        .projectId(testProject.getId())
        .status(EpicStatus.IN_PROGRESS)
        .build();

    mockMvc.perform(put("/api/epics/{id}", testEpic.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Epic")))
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateStatus_ShouldUpdateStatus() throws Exception {
    mockMvc.perform(patch("/api/epics/{id}/status", testEpic.getId())
            .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteEpic_ShouldDelete() throws Exception {
    mockMvc.perform(delete("/api/epics/{id}", testEpic.getId()))
        .andExpect(status().isNoContent());

    // Verify deletion (soft delete)
    mockMvc.perform(get("/api/epics/{id}", testEpic.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getEpicsByStatus_ShouldReturnFilteredEpics() throws Exception {
    mockMvc.perform(get("/api/epics/project/{projectId}/status/{status}", 
            testProject.getId(), "PLANNED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].status", is("PLANNED")));
  }
}
