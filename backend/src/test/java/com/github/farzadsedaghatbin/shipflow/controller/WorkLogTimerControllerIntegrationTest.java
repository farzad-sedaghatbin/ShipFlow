package com.github.farzadsedaghatbin.shipflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.StartTimerRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
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
public class WorkLogTimerControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private TaskRepository taskRepository;

  @Autowired
  private WorkLogTimerRepository timerRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  private User testUser;
  private Person testPerson;
  private Cycle testCycle;
  private Pitch testPitch;
  private Task testTask;

  @BeforeEach
  void setUp() {
    // Clean up
    timerRepository.deleteAll();
    workLogRepository.deleteAll();
    taskRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();

    // Create test person first
    testPerson = Person.builder().name("Test Person").email("timer-test@example.com").isActive(true)
        .createdAt(java.time.LocalDateTime.now()).build();
    testPerson = personRepository.save(testPerson);

    // Create test user with person
    testUser = User.builder().username("worklog-timer-test-user").password("password").role(UserRole.MEMBER)
        .person(testPerson).build();
    testUser = userRepository.save(testUser);

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now().minusDays(7))
        .endDate(LocalDate.now().plusDays(30))
        .phase(com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase.SHAPING_BUILDING).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testPitch = Pitch.builder().title("Test Pitch").appetiteDays(14).cycle(testCycle)
        .status(com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus.PENDING)
        .createdAt(java.time.LocalDateTime.now()).updatedAt(java.time.LocalDateTime.now()).build();
    testPitch = pitchRepository.save(testPitch);

    testTask = Task.builder().title("Test Task").cycle(testCycle).build();
    testTask = taskRepository.save(testTask);
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldStartTimerForPitch() throws Exception {
    StartTimerRequest request = StartTimerRequest.builder().pitchId(testPitch.getId()).note("Working on feature")
        .build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.personId").value(testPerson.getId()))
        .andExpect(jsonPath("$.pitchId").value(testPitch.getId()))
        .andExpect(jsonPath("$.pitchTitle").value("Test Pitch")).andExpect(jsonPath("$.taskId").doesNotExist())
        .andExpect(jsonPath("$.note").value("Working on feature"))
        .andExpect(jsonPath("$.elapsedSeconds").isNumber());
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldStartTimerForTask() throws Exception {
    StartTimerRequest request = StartTimerRequest.builder().taskId(testTask.getId()).note("Implementing feature")
        .build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.personId").value(testPerson.getId()))
        .andExpect(jsonPath("$.taskId").value(testTask.getId()))
        .andExpect(jsonPath("$.taskTitle").value("Test Task")).andExpect(jsonPath("$.pitchId").doesNotExist())
        .andExpect(jsonPath("$.note").value("Implementing feature"));
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldRejectStartingMultipleTimers() throws Exception {
    // Start first timer
    StartTimerRequest request1 = StartTimerRequest.builder().pitchId(testPitch.getId()).note("First timer").build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request1))).andExpect(status().isCreated());

    // Try to start second timer
    StartTimerRequest request2 = StartTimerRequest.builder().taskId(testTask.getId()).note("Second timer").build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request2))).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldStopTimerAndCreateWorkLog() throws Exception {
    // Start timer
    StartTimerRequest request = StartTimerRequest.builder().pitchId(testPitch.getId()).note("Working on feature")
        .build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

    // Wait a bit to have some elapsed time
    Thread.sleep(100);

    // Stop timer
    mockMvc.perform(post("/api/timers/stop")).andExpect(status().isOk()).andExpect(jsonPath("$.workLogId").exists())
        .andExpect(jsonPath("$.hoursSpent").isNumber()).andExpect(jsonPath("$.message").exists());

    // Verify work log was created
    long workLogCount = workLogRepository.count();
    assert workLogCount == 1;
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldGetActiveTimer() throws Exception {
    // Start timer
    StartTimerRequest request = StartTimerRequest.builder().pitchId(testPitch.getId()).note("Working on feature")
        .build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

    // Get active timer
    mockMvc.perform(get("/api/timers/active")).andExpect(status().isOk()).andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.pitchId").value(testPitch.getId()))
        .andExpect(jsonPath("$.note").value("Working on feature"))
        .andExpect(jsonPath("$.elapsedSeconds").isNumber());
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldReturnNotFoundWhenNoActiveTimer() throws Exception {
    mockMvc.perform(get("/api/timers/active")).andExpect(status().isOk()).andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldCancelTimer() throws Exception {
    // Start timer
    StartTimerRequest request = StartTimerRequest.builder().pitchId(testPitch.getId()).note("Working on feature")
        .build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

    // Cancel timer
    mockMvc.perform(delete("/api/timers/cancel")).andExpect(status().isNoContent());

    // Verify timer was deleted
    long timerCount = timerRepository.count();
    assert timerCount == 0;

    // Verify no work log was created
    long workLogCount = workLogRepository.count();
    assert workLogCount == 0;
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldRejectInvalidRequest() throws Exception {
    // Request without pitchId or taskId
    StartTimerRequest request = StartTimerRequest.builder().note("Invalid request").build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "worklog-timer-test-user")
  void shouldRejectBothPitchAndTask() throws Exception {
    // Request with both pitchId and taskId
    StartTimerRequest request = StartTimerRequest.builder().pitchId(testPitch.getId()).taskId(testTask.getId())
        .note("Invalid request").build();

    mockMvc.perform(post("/api/timers/start").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }
}
