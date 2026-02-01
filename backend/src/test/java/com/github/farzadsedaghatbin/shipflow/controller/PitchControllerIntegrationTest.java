package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
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
@WithMockUser(
    username = "admin",
    roles = {"ADMIN"})
class PitchControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private PitchRepository pitchRepository;

  @Autowired private CycleRepository cycleRepository;

  private Cycle testCycle;
  private Pitch testPitch;

  @BeforeEach
  void setUp() {
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();

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
            .status(PitchStatus.PENDING)
            .cycle(testCycle)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    testPitch = pitchRepository.save(testPitch);
  }

  @Test
  void getAllPitches_ShouldReturnPitches() throws Exception {
    mockMvc
        .perform(get("/api/pitches"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].title", is("Test Pitch")));
  }

  @Test
  void getPitchById_WhenExists_ShouldReturnPitch() throws Exception {
    mockMvc
        .perform(get("/api/pitches/{id}", testPitch.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testPitch.getId().intValue())))
        .andExpect(jsonPath("$.title", is("Test Pitch")));
  }

  @Test
  void getPitchById_WhenNotExists_ShouldReturn404() throws Exception {
    mockMvc.perform(get("/api/pitches/{id}", 9999L)).andExpect(status().isBadRequest());
  }

  @Test
  void createPitch_WithValidData_ShouldCreatePitch() throws Exception {
    CreatePitchRequest request =
        CreatePitchRequest.builder()
            .title("New Pitch")
            .description("New Description")
            .appetiteDays(4)
            .status(PitchStatus.PENDING)
            .cycleId(testCycle.getId())
            .build();

    mockMvc
        .perform(
            post("/api/pitches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("New Pitch")))
        .andExpect(jsonPath("$.description", is("New Description")));
  }

  @Test
  void updatePitch_WhenExists_ShouldUpdatePitch() throws Exception {
    CreatePitchRequest request =
        CreatePitchRequest.builder()
            .title("Updated Pitch")
            .description("Updated Description")
            .appetiteDays(3)
            .status(PitchStatus.IN_PROGRESS)
            .cycleId(testCycle.getId())
            .build();

    mockMvc
        .perform(
            put("/api/pitches/{id}", testPitch.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title", is("Updated Pitch")))
        .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
  }

  @Test
  void deletePitch_WhenExists_ShouldDeletePitch() throws Exception {
    mockMvc
        .perform(delete("/api/pitches/{id}", testPitch.getId()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/pitches/{id}", testPitch.getId())).andExpect(status().isBadRequest());
  }

  @Test
  void getPitchesByCycle_ShouldReturnPitchesForCycle() throws Exception {
    mockMvc
        .perform(get("/api/pitches/cycle/{cycleId}", testCycle.getId()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Test Pitch")));
  }
}
