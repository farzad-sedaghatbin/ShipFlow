package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogForSelfRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the "My Work Logs" feature (/api/worklogs/my endpoints) Tests the ability
 * for users to create and manage their own work logs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyWorkLogControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private WorkLogRepository workLogRepository;

  @Autowired private PersonRepository personRepository;

  @Autowired private PitchRepository pitchRepository;

  @Autowired private CycleRepository cycleRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private Cycle testCycle;
  private Person testPerson;
  private Person otherPerson;
  private Pitch testPitch;
  private Pitch otherPitch;
  private WorkLog testWorkLog;
  private WorkLog otherPersonWorkLog;
  private User testUser;

  @BeforeEach
  void setUp() {
    workLogRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();

    // Create test person linked to the mock user "testuser"
    testPerson =
        Person.builder()
            .name("Test User")
            .email("testuser@example.com")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    testPerson = personRepository.save(testPerson);

    // Create another person for ownership tests
    otherPerson =
        Person.builder()
            .name("Other User")
            .email("other@example.com")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    otherPerson = personRepository.save(otherPerson);

    // Create user linked to testPerson
    testUser =
        User.builder()
            .username("testuser")
            .password(passwordEncoder.encode("password"))
            .email("testuser@example.com")
            .role(UserRole.MEMBER)
            .person(testPerson)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    testUser = userRepository.save(testUser);

    // Create cycle and pitches
    testCycle =
        Cycle.builder()
            .name("Test Cycle")
            .phase(CyclePhase.BUILD)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusWeeks(6))
            .isActive(true)
            .build();
    testCycle = cycleRepository.save(testCycle);

    testPitch =
        Pitch.builder()
            .title("Test Pitch")
            .description("Test Description")
            .appetiteDays(6)
            .status(PitchStatus.IN_PROGRESS)
            .cycle(testCycle)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    testPitch = pitchRepository.save(testPitch);

    otherPitch =
        Pitch.builder()
            .title("Other Pitch")
            .description("Another Description")
            .appetiteDays(4)
            .status(PitchStatus.IN_PROGRESS)
            .cycle(testCycle)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    otherPitch = pitchRepository.save(otherPitch);

    // Create work log for test user
    testWorkLog =
        WorkLog.builder()
            .note("My Work")
            .hoursSpent(new BigDecimal("4.00"))
            .date(LocalDate.now())
            .person(testPerson)
            .pitch(testPitch)
            .build();
    testWorkLog = workLogRepository.save(testWorkLog);

    // Create work log for other person
    otherPersonWorkLog =
        WorkLog.builder()
            .note("Other's Work")
            .hoursSpent(new BigDecimal("3.00"))
            .date(LocalDate.now())
            .person(otherPerson)
            .pitch(testPitch)
            .build();
    otherPersonWorkLog = workLogRepository.save(otherPersonWorkLog);
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void getMyWorkLogs_ShouldReturnOnlyCurrentUserWorkLogs() throws Exception {
    mockMvc
        .perform(get("/api/worklogs/my"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].note", is("My Work")))
        .andExpect(jsonPath("$[0].personName", is("Test User")));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void getMyWorkLogsByCycle_ShouldReturnUserWorkLogsForCycle() throws Exception {
    mockMvc
        .perform(get("/api/worklogs/my/cycle/{cycleId}", testCycle.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].note", is("My Work")));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void getMyWorkLogsByDate_ShouldReturnUserWorkLogsForDate() throws Exception {
    mockMvc
        .perform(get("/api/worklogs/my/date/{date}", LocalDate.now().toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].note", is("My Work")));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void createMyWorkLog_WithValidData_ShouldCreateWorkLog() throws Exception {
    CreateWorkLogForSelfRequest request =
        CreateWorkLogForSelfRequest.builder()
            .pitchId(otherPitch.getId())
            .date(LocalDate.now())
            .hoursSpent(new BigDecimal("2.50"))
            .note("New self work log")
            .build();

    mockMvc
        .perform(
            post("/api/worklogs/my")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.note", is("New self work log")))
        .andExpect(jsonPath("$.personName", is("Test User")))
        .andExpect(jsonPath("$.hoursSpent", is(2.50)));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void createMyWorkLog_WithInvalidPitch_ShouldReturn400() throws Exception {
    CreateWorkLogForSelfRequest request =
        CreateWorkLogForSelfRequest.builder()
            .pitchId(9999L)
            .date(LocalDate.now())
            .hoursSpent(new BigDecimal("2.50"))
            .note("Invalid pitch")
            .build();

    mockMvc
        .perform(
            post("/api/worklogs/my")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void updateMyWorkLog_WhenOwned_ShouldUpdateWorkLog() throws Exception {
    CreateWorkLogForSelfRequest request =
        CreateWorkLogForSelfRequest.builder()
            .pitchId(testPitch.getId())
            .date(LocalDate.now())
            .hoursSpent(new BigDecimal("6.00"))
            .note("Updated my work")
            .build();

    mockMvc
        .perform(
            put("/api/worklogs/my/{id}", testWorkLog.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.note", is("Updated my work")))
        .andExpect(jsonPath("$.hoursSpent", is(6.00)));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void updateMyWorkLog_WhenNotOwned_ShouldReturn400() throws Exception {
    CreateWorkLogForSelfRequest request =
        CreateWorkLogForSelfRequest.builder()
            .pitchId(testPitch.getId())
            .date(LocalDate.now())
            .hoursSpent(new BigDecimal("6.00"))
            .note("Trying to update other's work")
            .build();

    mockMvc
        .perform(
            put("/api/worklogs/my/{id}", otherPersonWorkLog.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void deleteMyWorkLog_WhenOwned_ShouldDeleteWorkLog() throws Exception {
    mockMvc
        .perform(delete("/api/worklogs/my/{id}", testWorkLog.getId()))
        .andExpect(status().isNoContent());

    // Verify it's deleted
    mockMvc
        .perform(get("/api/worklogs/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  void deleteMyWorkLog_WhenNotOwned_ShouldReturn400() throws Exception {
    mockMvc
        .perform(delete("/api/worklogs/my/{id}", otherPersonWorkLog.getId()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getMyWorkLogs_WithoutAuthentication_ShouldReturn401() throws Exception {
    mockMvc.perform(get("/api/worklogs/my")).andExpect(status().isFound()); // 302 redirect to login
  }

  @Test
  void createMyWorkLog_WithoutAuthentication_ShouldReturn401() throws Exception {
    CreateWorkLogForSelfRequest request =
        CreateWorkLogForSelfRequest.builder()
            .pitchId(testPitch.getId())
            .date(LocalDate.now())
            .hoursSpent(new BigDecimal("2.50"))
            .note("Unauthorized")
            .build();

    mockMvc
        .perform(
            post("/api/worklogs/my")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isFound()); // 302 redirect to login
  }
}
