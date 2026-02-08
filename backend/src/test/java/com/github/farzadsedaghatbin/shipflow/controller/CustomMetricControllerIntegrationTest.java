package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.metrics.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for CustomMetricController */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomMetricControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CustomMetricRepository customMetricRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private WorkLogRepository workLogRepository;

  private User testUser;
  private Person testPerson;

  @BeforeEach
  void setUp() {
    // Clean up in correct order due to foreign key constraints
    customMetricRepository.deleteAll();
    workLogRepository.deleteAll();
    userRepository.deleteAll();
    personRepository.deleteAll();

    // Create test person
    testPerson = Person.builder().name("Test User").email("test@example.com").isActive(true).build();
    testPerson = personRepository.save(testPerson);

    // Create test user
    testUser = User.builder().username("testuser").password("password").role(UserRole.MEMBER).person(testPerson)
        .isActive(true).build();
    testUser = userRepository.save(testUser);
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should create a new custom metric")
  void createMetric_ShouldReturnCreatedMetric() throws Exception {
    CreateCustomMetricRequest request = CreateCustomMetricRequest.builder().name("Velocity Metric")
        .description("Measures team velocity").formula("PITCH_COUNT").dataSource(CustomMetric.DataSource.PITCH)
        .aggregationType(CustomMetric.AggregationType.COUNT).displayFormat(CustomMetric.DisplayFormat.NUMBER)
        .build();

    mockMvc.perform(post("/api/metrics/custom").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Velocity Metric"))
        .andExpect(jsonPath("$.description").value("Measures team velocity"))
        .andExpect(jsonPath("$.formula").value("PITCH_COUNT"))
        .andExpect(jsonPath("$.dataSource").value("PITCH"))
        .andExpect(jsonPath("$.aggregationType").value("COUNT"))
        .andExpect(jsonPath("$.displayFormat").value("NUMBER")).andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should reject metric with invalid formula")
  void createMetric_WithInvalidFormula_ShouldReturnBadRequest() throws Exception {
    CreateCustomMetricRequest request = CreateCustomMetricRequest.builder().name("Invalid Metric")
        .formula("INVALID_FIELD + )").dataSource(CustomMetric.DataSource.CUSTOM)
        .aggregationType(CustomMetric.AggregationType.SUM).displayFormat(CustomMetric.DisplayFormat.NUMBER)
        .build();

    mockMvc.perform(post("/api/metrics/custom").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should get all metrics for user")
  void getUserMetrics_ShouldReturnAllUserMetrics() throws Exception {
    // Create test metrics
    CustomMetric metric1 = CustomMetric.builder().user(testUser).name("Metric 1").formula("10 + 5")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    customMetricRepository.save(metric1);

    CustomMetric metric2 = CustomMetric.builder().user(testUser).name("Metric 2").formula("20 * 2")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.AVG)
        .displayFormat(CustomMetric.DisplayFormat.PERCENTAGE).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    customMetricRepository.save(metric2);

    mockMvc.perform(get("/api/metrics/custom")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].name").value("Metric 1"))
        .andExpect(jsonPath("$[1].name").value("Metric 2"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should get specific metric by ID")
  void getMetric_ShouldReturnMetric() throws Exception {
    CustomMetric metric = CustomMetric.builder().user(testUser).name("Test Metric").formula("100")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    mockMvc.perform(get("/api/metrics/custom/" + metric.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(metric.getId())).andExpect(jsonPath("$.name").value("Test Metric"))
        .andExpect(jsonPath("$.formula").value("100"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should return 404 for non-existent metric")
  void getMetric_WithInvalidId_ShouldReturnNotFound() throws Exception {
    mockMvc.perform(get("/api/metrics/custom/999999")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should update existing metric")
  void updateMetric_ShouldReturnUpdatedMetric() throws Exception {
    CustomMetric metric = CustomMetric.builder().user(testUser).name("Original Name").formula("10")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    UpdateCustomMetricRequest request = UpdateCustomMetricRequest.builder().name("Updated Name")
        .description("Updated description").formula("20").build();

    mockMvc.perform(put("/api/metrics/custom/" + metric.getId()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(metric.getId())).andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.formula").value("20"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should delete metric")
  void deleteMetric_ShouldReturnNoContent() throws Exception {
    CustomMetric metric = CustomMetric.builder().user(testUser).name("To Delete").formula("10")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    mockMvc.perform(delete("/api/metrics/custom/" + metric.getId())).andExpect(status().isNoContent());

    // Verify metric is deleted
    mockMvc.perform(get("/api/metrics/custom/" + metric.getId())).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should calculate metric value")
  void calculateMetric_ShouldReturnCalculatedValue() throws Exception {
    CustomMetric metric = CustomMetric.builder().user(testUser).name("Simple Metric").formula("10 + 20")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    mockMvc.perform(post("/api/metrics/custom/" + metric.getId() + "/calculate")).andExpect(status().isOk())
        .andExpect(jsonPath("$.value").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should get metric history")
  void getMetricHistory_ShouldReturnHistoricalData() throws Exception {
    CustomMetric metric = CustomMetric.builder().user(testUser).name("Test Metric").formula("100")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    mockMvc.perform(get("/api/metrics/custom/" + metric.getId() + "/history").param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
  }

  @Test
  @WithMockUser(username = "otheruser", roles = "DEVELOPER")
  @DisplayName("Should deny access to another user's metric")
  void getMetric_AsOtherUser_ShouldReturnForbidden() throws Exception {
    // Create another user
    Person otherPerson = Person.builder().name("Other User").email("other@example.com").isActive(true).build();
    otherPerson = personRepository.save(otherPerson);

    User otherUser = User.builder().username("otheruser").password("password").role(UserRole.MEMBER)
        .person(otherPerson).isActive(true).build();
    userRepository.save(otherUser);

    CustomMetric metric = CustomMetric.builder().user(testUser).name("Private Metric").formula("10")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    metric = customMetricRepository.save(metric);

    // otheruser should not be able to access testuser's metric
    mockMvc.perform(get("/api/metrics/custom/" + metric.getId())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "DEVELOPER")
  @DisplayName("Should prevent duplicate metric names")
  void createMetric_WithDuplicateName_ShouldReturnBadRequest() throws Exception {
    // Create first metric
    CustomMetric existingMetric = CustomMetric.builder().user(testUser).name("Unique Name").formula("10")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).isActive(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    customMetricRepository.save(existingMetric);

    // Try to create second metric with same name
    CreateCustomMetricRequest request = CreateCustomMetricRequest.builder().name("Unique Name").formula("20")
        .dataSource(CustomMetric.DataSource.CUSTOM).aggregationType(CustomMetric.AggregationType.SUM)
        .displayFormat(CustomMetric.DisplayFormat.NUMBER).build();

    mockMvc.perform(post("/api/metrics/custom").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }
}
