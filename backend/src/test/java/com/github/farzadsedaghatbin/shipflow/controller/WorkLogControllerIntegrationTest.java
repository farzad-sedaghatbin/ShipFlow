package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.math.BigDecimal;
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
class WorkLogControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WorkLogRepository workLogRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  private Cycle testCycle;
  private Team testTeam;
  private Person testPerson;
  private Pitch testPitch;
  private WorkLog testWorkLog;

  @BeforeEach
  void setUp() {
    workLogRepository.deleteAll();
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testTeam = Team.builder().name("Test Team").cycle(testCycle).build();
    testTeam = teamRepository.save(testTeam);

    testPerson = Person.builder().name("John Doe").email("john.doe@example.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();
    testPerson = personRepository.save(testPerson);

    User testUser = User.builder().username("workloguser").password("password").role(UserRole.MEMBER)
        .person(testPerson).isActive(true).build();
    testUser = userRepository.save(testUser);

    // Grant PITCH permissions for MEMBER role
    permissionRepository.save(Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.PITCH)
        .permissionType(PermissionType.READ).build());
    permissionRepository.save(Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.PITCH)
        .permissionType(PermissionType.CREATE).build());
    permissionRepository.save(Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.PITCH)
        .permissionType(PermissionType.UPDATE).build());
    permissionRepository.save(Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.PITCH)
        .permissionType(PermissionType.DELETE).build());

    testPitch = Pitch.builder().title("Test Pitch").description("Test Description").appetiteDays(6)
        .status(PitchStatus.IN_PROGRESS).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    testPitch = pitchRepository.save(testPitch);

    testWorkLog = WorkLog.builder().note("Test Work").hoursSpent(new BigDecimal("4.00")).date(LocalDate.now())
        .person(testPerson).pitch(testPitch).build();
    testWorkLog = workLogRepository.save(testWorkLog);
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void getAllWorkLogs_ShouldReturnWorkLogs() throws Exception {
    mockMvc.perform(get("/api/worklogs")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].note", is("Test Work")));
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void getWorkLogById_WhenExists_ShouldReturnWorkLog() throws Exception {
    mockMvc.perform(get("/api/worklogs/{id}", testWorkLog.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testWorkLog.getId().intValue())))
        .andExpect(jsonPath("$.note", is("Test Work")));
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void getWorkLogById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/worklogs/{id}", 9999L)).andExpect(status().isBadRequest());
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void createWorkLog_WithValidData_ShouldCreateWorkLog() throws Exception {
    CreateWorkLogRequest request = CreateWorkLogRequest.builder().note("New Work")
        .hoursSpent(new BigDecimal("2.50")).date(LocalDate.now()).personId(testPerson.getId())
        .pitchId(testPitch.getId()).build();

    mockMvc.perform(post("/api/worklogs").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.note", is("New Work")));
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void updateWorkLog_WhenExists_ShouldUpdateWorkLog() throws Exception {
    CreateWorkLogRequest request = CreateWorkLogRequest.builder().note("Updated Work")
        .hoursSpent(new BigDecimal("6.00")).date(LocalDate.now()).personId(testPerson.getId())
        .pitchId(testPitch.getId()).build();

    mockMvc.perform(put("/api/worklogs/{id}", testWorkLog.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.note", is("Updated Work")));
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void deleteWorkLog_WhenExists_ShouldDeleteWorkLog() throws Exception {
    mockMvc.perform(delete("/api/worklogs/{id}", testWorkLog.getId())).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/worklogs/{id}", testWorkLog.getId())).andExpect(status().isBadRequest());
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void getWorkLogsByPerson_ShouldReturnWorkLogsForPerson() throws Exception {
    mockMvc.perform(get("/api/worklogs/person/{personId}", testPerson.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].note", is("Test Work")));
  }

  @WithMockUser(username = "workloguser", roles = "MEMBER")
  @Test
  void getWorkLogsByPitch_ShouldReturnWorkLogsForPitch() throws Exception {
    mockMvc.perform(get("/api/worklogs/pitch/{pitchId}", testPitch.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].note", is("Test Work")));
  }
}
