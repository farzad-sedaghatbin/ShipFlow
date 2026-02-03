package com.github.farzadsedaghatbin.shipflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class CyclePitchCountIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  private Project testProject;
  private Cycle testCycle;
  private User testUser;

  @BeforeEach
  void setUp() {
    // Clean up
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = User.builder().username("testuser").email("test@example.com").role(UserRole.ADMIN).build();
    testUser = userRepository.save(testUser);

    // Create test project
    testProject = Project.builder().name("Test Project").projectKey("TEST").description("Test Description")
        .isActive(true).owner(testUser).build();
    testProject = projectRepository.save(testProject);

    // Create test cycle
    testCycle = Cycle.builder().name("Test Cycle").project(testProject).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).phase(CyclePhase.BUILD).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"ADMIN"})
  void getCycleById_ShouldReturnCorrectPitchCount_ExcludingDeletedPitches() throws Exception {
    // Arrange - Create pitches (2 active, 1 deleted)
    Pitch activePitch1 = createPitch("Active Pitch 1", PitchStatus.PENDING, false);
    Pitch activePitch2 = createPitch("Active Pitch 2", PitchStatus.IN_PROGRESS, false);
    Pitch deletedPitch = createPitch("Deleted Pitch", PitchStatus.DONE, true);

    pitchRepository.save(activePitch1);
    pitchRepository.save(activePitch2);
    pitchRepository.save(deletedPitch);

    // Act & Assert
    MvcResult result = mockMvc
        .perform(get("/api/cycles/{id}", testCycle.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    CycleDTO cycleDTO = objectMapper.readValue(jsonResponse, CycleDTO.class);

    // Should only count the 2 active pitches, not the deleted one
    assertThat(cycleDTO.getPitchCount()).isEqualTo(2);
    assertThat(cycleDTO.getId()).isEqualTo(testCycle.getId());
    assertThat(cycleDTO.getName()).isEqualTo("Test Cycle");
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"ADMIN"})
  void getActiveCycles_ShouldReturnCorrectPitchCount_ExcludingDeletedPitches() throws Exception {
    // Arrange - Create pitches
    Pitch activePitch = createPitch("Active Pitch", PitchStatus.PENDING, false);
    Pitch deletedPitch1 = createPitch("Deleted Pitch 1", PitchStatus.DONE, true);
    Pitch deletedPitch2 = createPitch("Deleted Pitch 2", PitchStatus.CANCELLED, true);

    pitchRepository.save(activePitch);
    pitchRepository.save(deletedPitch1);
    pitchRepository.save(deletedPitch2);

    // Act & Assert
    MvcResult result = mockMvc.perform(get("/api/cycles/active").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    CycleDTO[] cycles = objectMapper.readValue(jsonResponse, CycleDTO[].class);

    assertThat(cycles).hasSize(1);
    // Should only count the 1 active pitch, not the 2 deleted ones
    assertThat(cycles[0].getPitchCount()).isEqualTo(1);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"ADMIN"})
  void getAllCycles_ShouldReturnCorrectPitchCount_WhenNoPitchesExist() throws Exception {
    // Act & Assert - No pitches created
    MvcResult result = mockMvc.perform(get("/api/cycles").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    CycleDTO[] cycles = objectMapper.readValue(jsonResponse, CycleDTO[].class);

    assertThat(cycles).hasSize(1);
    assertThat(cycles[0].getPitchCount()).isEqualTo(0);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"ADMIN"})
  void getAllCycles_ShouldReturnZeroPitchCount_WhenAllPitchesAreDeleted() throws Exception {
    // Arrange - Create and delete all pitches
    Pitch pitch1 = createPitch("Pitch 1", PitchStatus.PENDING, true);
    Pitch pitch2 = createPitch("Pitch 2", PitchStatus.IN_PROGRESS, true);
    Pitch pitch3 = createPitch("Pitch 3", PitchStatus.DONE, true);

    pitchRepository.save(pitch1);
    pitchRepository.save(pitch2);
    pitchRepository.save(pitch3);

    // Act & Assert
    MvcResult result = mockMvc.perform(get("/api/cycles").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    String jsonResponse = result.getResponse().getContentAsString();
    CycleDTO[] cycles = objectMapper.readValue(jsonResponse, CycleDTO[].class);

    assertThat(cycles).hasSize(1);
    // All pitches are deleted, so count should be 0
    assertThat(cycles[0].getPitchCount()).isEqualTo(0);
  }

  private Pitch createPitch(String title, PitchStatus status, boolean isDeleted) {
    Pitch pitch = Pitch.builder().title(title).description("Test description for " + title).appetiteDays(6)
        .cycle(testCycle).status(status).problemStatement("Test problem").solution("Test solution").build();

    if (isDeleted) {
      pitch.setDeletedAt(LocalDateTime.now());
      pitch.setDeletedBy(testUser);
    }

    return pitch;
  }
}
