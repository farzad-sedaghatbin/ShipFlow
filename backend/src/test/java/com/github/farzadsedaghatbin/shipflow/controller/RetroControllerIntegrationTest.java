package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroItemRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@WithMockUser(
    username = "admin",
    roles = {"ADMIN"})
class RetroControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private CycleRepository cycleRepository;

  @Autowired private RetroRepository retroRepository;

  @Autowired private RetroItemRepository retroItemRepository;

  @Autowired private UserRepository userRepository;

  private Project testProject;
  private Cycle testCycle;
  private Retrospective testRetro;
  private User adminUser;

  @BeforeEach
  void setUp() {
    // Clean up first
    retroItemRepository.deleteAll();
    retroRepository.deleteAll();

    // Ensure admin user exists for authentication
    adminUser =
        userRepository
            .findByUsername("admin")
            .orElseGet(
                () ->
                    userRepository.save(
                        User.builder()
                            .username("admin")
                            .password("password") // In real test, would be encoded
                            .email("admin@test.com")
                            .role(UserRole.ADMIN)
                            .build()));

    testProject =
        projectRepository.save(
            Project.builder()
                .name("Test Project")
                .projectKey("RETRO")
                .isActive(true)
                .enableRetrospectives(true)
                .createdAt(LocalDateTime.now())
                .build());

    testCycle =
        cycleRepository.save(
            Cycle.builder()
                .name("Test Cycle")
                .project(testProject)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .phase(CyclePhase.BUILD)
                .isActive(true)
                .build());
  }

  @Nested
  @DisplayName("Retro CRUD Operations")
  class RetroCrudTests {

    @Test
    @DisplayName("Create retrospective succeeds")
    void createRetroSucceeds() throws Exception {
      CreateRetroRequest request =
          CreateRetroRequest.builder()
              .title("Test Retrospective")
              .notes("Test notes")
              .cycleId(testCycle.getId())
              .projectId(testProject.getId())
              .build();

      mockMvc
          .perform(
              post("/api/retros")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title", is("Test Retrospective")))
          .andExpect(jsonPath("$.status", is("DRAFT")))
          .andExpect(jsonPath("$.cycleId", is(testCycle.getId().intValue())));
    }

    @Test
    @DisplayName("Get retrospective by ID succeeds")
    void getRetroByIdSucceeds() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Test Retro")
                  .status(RetroStatus.DRAFT)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(get("/api/retros/" + retro.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title", is("Test Retro")))
          .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    @DisplayName("Get retrospectives by project succeeds")
    void getRetrosByProjectSucceeds() throws Exception {
      retroRepository.save(
          Retrospective.builder()
              .title("Retro 1")
              .status(RetroStatus.DRAFT)
              .cycle(testCycle)
              .project(testProject)
              .createdAt(LocalDateTime.now())
              .build());

      retroRepository.save(
          Retrospective.builder()
              .title("Retro 2")
              .status(RetroStatus.OPEN)
              .cycle(testCycle)
              .project(testProject)
              .createdAt(LocalDateTime.now())
              .build());

      mockMvc
          .perform(get("/api/retros/project/" + testProject.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Update retrospective succeeds")
    void updateRetroSucceeds() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Original Title")
                  .status(RetroStatus.DRAFT)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      UpdateRetroRequest updateRequest =
          UpdateRetroRequest.builder().title("Updated Title").notes("Updated notes").build();

      mockMvc
          .perform(
              put("/api/retros/" + retro.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(updateRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    @DisplayName("Delete retrospective succeeds for admin")
    void deleteRetroSucceeds() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("To Delete")
                  .status(RetroStatus.DRAFT)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc.perform(delete("/api/retros/" + retro.getId())).andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("Retro Status Transitions")
  class RetroStatusTests {

    @Test
    @DisplayName("Open retrospective succeeds")
    void openRetroSucceeds() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Draft Retro")
                  .status(RetroStatus.DRAFT)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(post("/api/retros/" + retro.getId() + "/open"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    @DisplayName("Close retrospective succeeds")
    void closeRetroSucceeds() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Open Retro")
                  .status(RetroStatus.OPEN)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(post("/api/retros/" + retro.getId() + "/close"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status", is("CLOSED")))
          .andExpect(jsonPath("$.closedAt", notNullValue()));
    }

    @Test
    @DisplayName("Cannot open closed retrospective")
    void cannotOpenClosedRetro() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Closed Retro")
                  .status(RetroStatus.CLOSED)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .closedAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(post("/api/retros/" + retro.getId() + "/open"))
          .andExpect(status().is5xxServerError());
    }
  }

  @Nested
  @DisplayName("Retro Item Operations")
  class RetroItemTests {

    private Retrospective openRetro;

    @BeforeEach
    void setUpRetro() {
      openRetro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Open Retro")
                  .status(RetroStatus.OPEN)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());
    }

    @Test
    @DisplayName("Add item to retrospective succeeds")
    void addItemSucceeds() throws Exception {
      CreateRetroItemRequest request =
          CreateRetroItemRequest.builder()
              .content("Something went well")
              .columnType(RetroColumnType.WENT_WELL)
              .retrospectiveId(openRetro.getId())
              .build();

      mockMvc
          .perform(
              post("/api/retros/items")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content", is("Something went well")))
          .andExpect(jsonPath("$.columnType", is("WENT_WELL")));
    }

    @Test
    @DisplayName("Add anonymous item to retrospective succeeds")
    void addAnonymousItemSucceeds() throws Exception {
      CreateRetroItemRequest request =
          CreateRetroItemRequest.builder()
              .content("Sensitive feedback that should be anonymous")
              .columnType(RetroColumnType.DID_NOT_GO_WELL)
              .retrospectiveId(openRetro.getId())
              .isAnonymous(true)
              .build();

      mockMvc
          .perform(
              post("/api/retros/items")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content", is("Sensitive feedback that should be anonymous")))
          .andExpect(jsonPath("$.columnType", is("DID_NOT_GO_WELL")))
          .andExpect(jsonPath("$.isAnonymous", is(true)))
          .andExpect(jsonPath("$.author").doesNotExist());
    }

    @Test
    @DisplayName("Add non-anonymous item shows author")
    void addNonAnonymousItemShowsAuthor() throws Exception {
      CreateRetroItemRequest request =
          CreateRetroItemRequest.builder()
              .content("Public feedback with attribution")
              .columnType(RetroColumnType.WENT_WELL)
              .retrospectiveId(openRetro.getId())
              .isAnonymous(false)
              .build();

      mockMvc
          .perform(
              post("/api/retros/items")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content", is("Public feedback with attribution")))
          .andExpect(jsonPath("$.isAnonymous", is(false)))
          .andExpect(jsonPath("$.authorId", notNullValue()))
          .andExpect(jsonPath("$.authorName", notNullValue()));
    }

    @Test
    @DisplayName("Get items by retrospective succeeds")
    void getItemsSucceeds() throws Exception {
      retroItemRepository.save(
          RetroItem.builder()
              .content("Item 1")
              .columnType(RetroColumnType.WENT_WELL)
              .retrospective(openRetro)
              .createdAt(LocalDateTime.now())
              .build());

      retroItemRepository.save(
          RetroItem.builder()
              .content("Item 2")
              .columnType(RetroColumnType.TRY_NEXT)
              .retrospective(openRetro)
              .createdAt(LocalDateTime.now())
              .build());

      mockMvc
          .perform(get("/api/retros/" + openRetro.getId() + "/items"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Update item succeeds")
    void updateItemSucceeds() throws Exception {
      RetroItem item =
          retroItemRepository.save(
              RetroItem.builder()
                  .content("Original")
                  .columnType(RetroColumnType.ACTIONS)
                  .retrospective(openRetro)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(
              put("/api/retros/items/" + item.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("content", "Updated"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content", is("Updated")));
    }

    @Test
    @DisplayName("Delete item succeeds")
    void deleteItemSucceeds() throws Exception {
      RetroItem item =
          retroItemRepository.save(
              RetroItem.builder()
                  .content("To delete")
                  .columnType(RetroColumnType.DID_NOT_GO_WELL)
                  .retrospective(openRetro)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(delete("/api/retros/items/" + item.getId()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cannot add item to closed retrospective")
    void cannotAddItemToClosedRetro() throws Exception {
      openRetro.setStatus(RetroStatus.CLOSED);
      openRetro.setClosedAt(LocalDateTime.now());
      retroRepository.save(openRetro);

      CreateRetroItemRequest request =
          CreateRetroItemRequest.builder()
              .content("New item")
              .columnType(RetroColumnType.WENT_WELL)
              .retrospectiveId(openRetro.getId())
              .build();

      mockMvc
          .perform(
              post("/api/retros/items")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().is5xxServerError());
    }
  }

  @Nested
  @DisplayName("Cycle Retro Status")
  class CycleRetroStatusTests {

    @Test
    @DisplayName("Get cycle status with no retros")
    void getCycleStatusNoRetros() throws Exception {
      mockMvc
          .perform(get("/api/retros/cycle/" + testCycle.getId() + "/status"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalRetros", is(0)))
          .andExpect(jsonPath("$.closedRetros", is(0)))
          .andExpect(jsonPath("$.canCloseCycle", is(false)));
    }

    @Test
    @DisplayName("Get cycle status with closed retro")
    void getCycleStatusWithClosedRetro() throws Exception {
      retroRepository.save(
          Retrospective.builder()
              .title("Closed Retro")
              .status(RetroStatus.CLOSED)
              .cycle(testCycle)
              .project(testProject)
              .createdAt(LocalDateTime.now())
              .closedAt(LocalDateTime.now())
              .build());

      mockMvc
          .perform(get("/api/retros/cycle/" + testCycle.getId() + "/status"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalRetros", is(1)))
          .andExpect(jsonPath("$.closedRetros", is(1)))
          .andExpect(jsonPath("$.canCloseCycle", is(true)));
    }

    @Test
    @DisplayName("Can close cycle check endpoint")
    void canCloseCycleEndpoint() throws Exception {
      retroRepository.save(
          Retrospective.builder()
              .title("Closed Retro")
              .status(RetroStatus.CLOSED)
              .cycle(testCycle)
              .project(testProject)
              .createdAt(LocalDateTime.now())
              .closedAt(LocalDateTime.now())
              .build());

      mockMvc
          .perform(get("/api/retros/cycle/" + testCycle.getId() + "/can-close"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.canClose", is(true)));
    }
  }

  @Nested
  @DisplayName("Project Setting Tests")
  class ProjectSettingTests {

    @Test
    @DisplayName("Check if retrospectives enabled")
    void checkRetrosEnabled() throws Exception {
      mockMvc
          .perform(get("/api/retros/project/" + testProject.getId() + "/enabled"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    @DisplayName("Disable retrospectives for project")
    void disableRetros() throws Exception {
      mockMvc
          .perform(
              put("/api/retros/project/" + testProject.getId() + "/enabled")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    @DisplayName("Access blocked when retros disabled")
    void accessBlockedWhenDisabled() throws Exception {
      testProject.setEnableRetrospectives(false);
      projectRepository.save(testProject);

      mockMvc
          .perform(get("/api/retros/project/" + testProject.getId()))
          .andExpect(status().is5xxServerError());
    }
  }

  @Nested
  @DisplayName("Authorization Tests")
  class AuthorizationTests {

    @Test
    @WithMockUser(
        username = "user",
        roles = {"USER"})
    @DisplayName("Non-admin cannot delete retrospective")
    void nonAdminCannotDelete() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Cannot Delete")
                  .status(RetroStatus.DRAFT)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc.perform(delete("/api/retros/" + retro.getId())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
        username = "user",
        roles = {"USER"})
    @DisplayName("Non-admin cannot close retrospective")
    void nonAdminCannotClose() throws Exception {
      Retrospective retro =
          retroRepository.save(
              Retrospective.builder()
                  .title("Cannot Close")
                  .status(RetroStatus.OPEN)
                  .cycle(testCycle)
                  .project(testProject)
                  .createdAt(LocalDateTime.now())
                  .build());

      mockMvc
          .perform(post("/api/retros/" + retro.getId() + "/close"))
          .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
        username = "user",
        roles = {"USER"})
    @DisplayName("Non-admin cannot toggle project setting")
    void nonAdminCannotToggleSetting() throws Exception {
      mockMvc
          .perform(
              put("/api/retros/project/" + testProject.getId() + "/enabled")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
          .andExpect(status().isForbidden());
    }
  }
}
