package com.github.farzadsedaghatbin.shipflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
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
class HillChartControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private HillChartPointRepository hillChartPointRepository;

  private Pitch testPitch;
  private HillChartPoint testPoint;

  @BeforeEach
  void setUp() {
    hillChartPointRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();
    projectRepository.deleteAll();

    Project project = new Project();
    project.setName("Test Project");
    project.setProjectKey("TP");
    project.setIsActive(true);
    project = projectRepository.save(project);

    Cycle cycle = new Cycle();
    cycle.setProject(project);
    cycle.setName("Test Cycle");
    cycle.setStartDate(LocalDate.now());
    cycle.setEndDate(LocalDate.now().plusDays(42));
    cycle.setPhase(CyclePhase.SHAPING_BUILDING);
    cycle.setIsActive(true);
    cycle = cycleRepository.save(cycle);

    testPitch = new Pitch();
    testPitch.setCycle(cycle);
    testPitch.setTitle("Test Pitch");
    testPitch.setAppetiteDays(5);
    testPitch.setStatus(PitchStatus.IN_PROGRESS);
    testPitch = pitchRepository.save(testPitch);

    testPoint = new HillChartPoint();
    testPoint.setPitch(testPitch);
    testPoint.setScope("Test Scope");
    testPoint.setDescription("Test Description");
    testPoint.setPosition(50);
    testPoint = hillChartPointRepository.save(testPoint);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createHillChartPoint_WithValidData_ShouldCreatePoint() throws Exception {
    CreateHillChartPointRequest request = CreateHillChartPointRequest.builder().pitchId(testPitch.getId())
        .scope("Feature A").description("Implementing feature A").position(25).build();

    mockMvc.perform(post("/api/hill-chart").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.scope").value("Feature A")).andExpect(jsonPath("$.position").value(25));
  }

  @Test
  @WithMockUser(roles = "DEVELOPER")
  void getAllHillChartPoints_ShouldReturnPoints() throws Exception {
    mockMvc.perform(get("/api/hill-chart")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
  }

  @Test
  @WithMockUser(roles = "DEVELOPER")
  void getHillChartPointById_WhenExists_ShouldReturnPoint() throws Exception {
    mockMvc.perform(get("/api/hill-chart/" + testPoint.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testPoint.getId()));
  }

  @Test
  @WithMockUser(roles = "DEVELOPER")
  void getHillChartPointsByPitch_ShouldReturnPointsForPitch() throws Exception {
    mockMvc.perform(get("/api/hill-chart/pitch/" + testPitch.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @WithMockUser(roles = "PROJECT_MANAGER")
  void updateHillChartPoint_WhenExists_ShouldUpdatePoint() throws Exception {
    UpdateHillChartPointRequest request = UpdateHillChartPointRequest.builder().position(75).build();

    mockMvc.perform(put("/api/hill-chart/" + testPoint.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteHillChartPoint_WhenExists_ShouldDeletePoint() throws Exception {
    mockMvc.perform(delete("/api/hill-chart/" + testPoint.getId())).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "DEVELOPER")
  void createHillChartPoint_AsDeveloper_ShouldReturn403() throws Exception {
    CreateHillChartPointRequest request = CreateHillChartPointRequest.builder().pitchId(testPitch.getId())
        .scope("Feature B").description("Test").position(50).build();

    mockMvc.perform(post("/api/hill-chart").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "QA")
  void deleteHillChartPoint_AsQA_ShouldReturn403() throws Exception {
    mockMvc.perform(delete("/api/hill-chart/" + testPoint.getId())).andExpect(status().isForbidden());
  }
}
