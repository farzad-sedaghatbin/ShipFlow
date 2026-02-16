package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.util.List;
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

/**
 * Integration tests for OrganizationSettingsController. Tests cover: -
 * Authentication and authorization - GET, PUT, POST endpoints - Request
 * validation - Response formats - Database persistence
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OrganizationSettingsController Integration Tests")
class OrganizationSettingsControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private OrganizationSettingsRepository settingsRepository;

  @BeforeEach
  void setUp() {
    // Clean up before each test
    settingsRepository.deleteAll();
  }

  @Test
  @DisplayName("GET /api/admin/settings - Should redirect when not authenticated")
  void getSettings_Unauthenticated_ShouldRedirect() throws Exception {
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(username = "user", roles = {"DEVELOPER"})
  @DisplayName("GET /api/admin/settings - Should return 403 when not admin")
  void getSettings_NonAdmin_ShouldReturn403() throws Exception {
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("GET /api/admin/settings - Should return default settings when none exist")
  void getSettings_NoExisting_ShouldReturnDefaults() throws Exception {
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.organizationName", is("My Organization")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(6)))
        .andExpect(jsonPath("$.defaultCooldownWeeks", is(2)))
        .andExpect(jsonPath("$.riskThresholds.lowMax", is(30)))
        .andExpect(jsonPath("$.riskThresholds.mediumMax", is(60)))
        .andExpect(jsonPath("$.riskThresholds.highMax", is(85)))
        .andExpect(jsonPath("$.taskCategories", hasSize(2)))
        .andExpect(jsonPath("$.pitchCategories", hasSize(4))).andExpect(jsonPath("$.timeZone", is("UTC")))
        .andExpect(jsonPath("$.dateFormat", is("MM/DD/YYYY")))
        .andExpect(jsonPath("$.enableNotifications", is(true)))
        .andExpect(jsonPath("$.enableAIFeatures", is(true))).andExpect(jsonPath("$.updatedBy", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("GET /api/admin/settings - Should return existing settings")
  void getSettings_Existing_ShouldReturnSettings() throws Exception {
    // Given - Create existing settings
    OrganizationSettings existing = OrganizationSettings.builder().organizationName("Test Org")
        .defaultCycleLengthWeeks(8).defaultCooldownWeeks(3)
        .riskThresholdsJson("{\"lowMax\":25,\"mediumMax\":55,\"highMax\":80}").taskCategoriesJson("[]")
        .pitchCategoriesJson("[]").timeZone("America/New_York").dateFormat("DD/MM/YYYY")
        .enableNotifications(false).enableAIFeatures(false).enableWiseArchitecture(false).updatedBy("system").build();
    settingsRepository.save(existing);

    // When/Then
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("Test Org")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(8)))
        .andExpect(jsonPath("$.defaultCooldownWeeks", is(3)))
        .andExpect(jsonPath("$.riskThresholds.lowMax", is(25)))
        .andExpect(jsonPath("$.timeZone", is("America/New_York")))
        .andExpect(jsonPath("$.dateFormat", is("DD/MM/YYYY")))
        .andExpect(jsonPath("$.enableNotifications", is(false)))
        .andExpect(jsonPath("$.enableAIFeatures", is(false)));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should update organization name")
  void updateSettings_OrganizationName_ShouldUpdate() throws Exception {
    // Given
    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
        .organizationName("Updated Organization").build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("Updated Organization")))
        .andExpect(jsonPath("$.updatedBy", is("admin")));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should update cycle configuration")
  void updateSettings_CycleConfiguration_ShouldUpdate() throws Exception {
    // Given
    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
        .defaultCycleLengthWeeks(10).defaultCooldownWeeks(4).build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(10)))
        .andExpect(jsonPath("$.defaultCooldownWeeks", is(4)));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should update risk thresholds")
  void updateSettings_RiskThresholds_ShouldUpdate() throws Exception {
    // Given
    OrganizationSettingsDTO.RiskThresholds newThresholds = OrganizationSettingsDTO.RiskThresholds.builder()
        .lowMax(20).mediumMax(50).highMax(75).build();

    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
        .riskThresholds(newThresholds).build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.riskThresholds.lowMax", is(20)))
        .andExpect(jsonPath("$.riskThresholds.mediumMax", is(50)))
        .andExpect(jsonPath("$.riskThresholds.highMax", is(75)));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should update task categories")
  void updateSettings_TaskCategories_ShouldUpdate() throws Exception {
    // Given
    List<OrganizationSettingsDTO.CategoryConfig> categories = List
        .of(OrganizationSettingsDTO.CategoryConfig.builder().name("CUSTOM_TASK").description("Custom task type")
            .color("#FF0000").isActive(true).order(1).build());

    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
        .taskCategories(categories).build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.taskCategories", hasSize(1)))
        .andExpect(jsonPath("$.taskCategories[0].name", is("CUSTOM_TASK")))
        .andExpect(jsonPath("$.taskCategories[0].color", is("#FF0000")));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should update feature toggles")
  void updateSettings_FeatureToggles_ShouldUpdate() throws Exception {
    // Given
    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
        .enableNotifications(false).enableAIFeatures(false).build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.enableNotifications", is(false)))
        .andExpect(jsonPath("$.enableAIFeatures", is(false)));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should perform partial update")
  void updateSettings_PartialUpdate_ShouldUpdateOnlyProvidedFields() throws Exception {
    // Given - First create settings
    UpdateOrganizationSettingsRequest initial = UpdateOrganizationSettingsRequest.builder()
        .organizationName("Initial Org").defaultCycleLengthWeeks(6).build();
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(initial)));

    // Then - Update only organization name
    UpdateOrganizationSettingsRequest partial = UpdateOrganizationSettingsRequest.builder()
        .organizationName("Partial Update").build();

    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(partial))).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("Partial Update")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(6))); // Should remain unchanged
  }

  @Test
  @WithMockUser(username = "specific-admin", roles = {"ADMIN"})
  @DisplayName("PUT /api/admin/settings - Should track username in updatedBy")
  void updateSettings_ShouldTrackUsername() throws Exception {
    // Given
    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder().organizationName("Test")
        .build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.updatedBy", is("specific-admin")));
  }

  @Test
  @WithMockUser(username = "user", roles = {"DEVELOPER"})
  @DisplayName("PUT /api/admin/settings - Should return 403 for non-admin")
  void updateSettings_NonAdmin_ShouldReturn403() throws Exception {
    // Given
    UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder().organizationName("Test")
        .build();

    // When/Then
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("POST /api/admin/settings/reset - Should reset to defaults")
  void resetSettings_ShouldResetToDefaults() throws Exception {
    // Given - Create custom settings first
    UpdateOrganizationSettingsRequest custom = UpdateOrganizationSettingsRequest.builder()
        .organizationName("Custom Org").defaultCycleLengthWeeks(10).enableNotifications(false).build();
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(custom)));

    // When - Reset to defaults
    mockMvc.perform(post("/api/admin/settings/reset")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("My Organization")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(6)))
        .andExpect(jsonPath("$.defaultCooldownWeeks", is(2)))
        .andExpect(jsonPath("$.enableNotifications", is(true)))
        .andExpect(jsonPath("$.enableAIFeatures", is(true)))
        .andExpect(jsonPath("$.riskThresholds.lowMax", is(30)))
        .andExpect(jsonPath("$.taskCategories", hasSize(2)))
        .andExpect(jsonPath("$.pitchCategories", hasSize(4))).andExpect(jsonPath("$.updatedBy", is("admin")));
  }

  @Test
  @WithMockUser(username = "user", roles = {"DEVELOPER"})
  @DisplayName("POST /api/admin/settings/reset - Should return 403 for non-admin")
  void resetSettings_NonAdmin_ShouldReturn403() throws Exception {
    mockMvc.perform(post("/api/admin/settings/reset")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/admin/settings/reset - Should redirect when not authenticated")
  void resetSettings_Unauthenticated_ShouldRedirect() throws Exception {
    mockMvc.perform(post("/api/admin/settings/reset")).andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("Integration test - Full lifecycle: create, update, reset")
  void fullLifecycle_CreateUpdateReset_ShouldWork() throws Exception {
    // 1. Get initial defaults
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("My Organization")));

    // 2. Update settings
    UpdateOrganizationSettingsRequest update = UpdateOrganizationSettingsRequest.builder()
        .organizationName("Updated Org").defaultCycleLengthWeeks(8).build();
    mockMvc.perform(put("/api/admin/settings").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(update))).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("Updated Org")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(8)));

    // 3. Verify update persisted
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("Updated Org")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(8)));

    // 4. Reset to defaults
    mockMvc.perform(post("/api/admin/settings/reset")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("My Organization")))
        .andExpect(jsonPath("$.defaultCycleLengthWeeks", is(6)));

    // 5. Verify reset persisted
    mockMvc.perform(get("/api/admin/settings")).andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName", is("My Organization")));
  }
}
