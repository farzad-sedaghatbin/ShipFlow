package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.service.teams.TeamsIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TeamsIntegrationController
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.chat.enabled=false"
})
class TeamsIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamsIntegrationService teamsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createConfiguration_ShouldReturnCreatedConfiguration() throws Exception {
        // Given
        CreateTeamsConfigurationRequest request = CreateTeamsConfigurationRequest.builder()
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/test")
                .defaultChannel("General")
                .isEnabled(true)
                .build();

        TeamsConfigurationDTO response = TeamsConfigurationDTO.builder()
                .id(1L)
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/test")
                .defaultChannel("General")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamsService.createOrUpdateConfiguration(any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/teams/configurations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantName").value("Test Tenant"))
                .andExpect(jsonPath("$.webhookUrl").value("https://outlook.office.com/webhook/test"));
    }

    @Test
    @WithMockUser
    void getAllConfigurations_ShouldReturnList() throws Exception {
        // Given
        TeamsConfigurationDTO config = TeamsConfigurationDTO.builder()
                .id(1L)
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/test")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamsService.getAllConfigurations()).thenReturn(List.of(config));

        // When/Then
        mockMvc.perform(get("/api/teams/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantName").value("Test Tenant"));
    }

    @Test
    void getActiveConfiguration_WhenExists_ShouldReturnConfiguration() throws Exception {
        // Given
        TeamsConfigurationDTO config = TeamsConfigurationDTO.builder()
                .id(1L)
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/test")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamsService.getActiveConfiguration()).thenReturn(Optional.of(config));

        // When/Then
        mockMvc.perform(get("/api/teams/configurations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("Test Tenant"));
    }

    @Test
    void getActiveConfiguration_WhenNotExists_ShouldReturnNotFound() throws Exception {
        // Given
        when(teamsService.getActiveConfiguration()).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/teams/configurations/active"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteConfiguration_ShouldReturnNoContent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/teams/configurations/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(teamsService).deleteConfiguration(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createChannelConfig_ShouldReturnCreatedConfig() throws Exception {
        // Given
        CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder()
                .channelName("dev-team")
                .notifyTaskAssigned(true)
                .notifyTaskCompleted(true)
                .build();

        TeamsChannelConfigDTO response = TeamsChannelConfigDTO.builder()
                .id(1L)
                .teamsConfigId(1L)
                .channelName("dev-team")
                .notifyTaskAssigned(true)
                .notifyTaskCompleted(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamsService.createOrUpdateChannelConfig(eq(1L), any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/teams/configurations/1/channels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channelName").value("dev-team"));
    }

    @Test
    void getChannelConfigs_ShouldReturnList() throws Exception {
        // Given
        TeamsChannelConfigDTO channel = TeamsChannelConfigDTO.builder()
                .id(1L)
                .teamsConfigId(1L)
                .channelName("dev-team")
                .notifyTaskAssigned(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamsService.getChannelConfigs(1L)).thenReturn(List.of(channel));

        // When/Then
        mockMvc.perform(get("/api/teams/configurations/1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelName").value("dev-team"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteChannelConfig_ShouldReturnNoContent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/teams/channels/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(teamsService).deleteChannelConfig(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendTestNotification_ShouldReturnSuccess() throws Exception {
        // Given
        TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
                .message("Test notification")
                .build();

        doNothing().when(teamsService).sendTestNotification(eq(1L), any());

        // When/Then
        mockMvc.perform(post("/api/teams/configurations/1/test")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Test notification sent successfully"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getNotificationHistory_ShouldReturnHistory() throws Exception {
        // Given
        TeamsNotificationHistoryDTO history = TeamsNotificationHistoryDTO.builder()
                .id(1L)
                .teamsConfigId(1L)
                .channelName("General")
                .notificationType("TASK_ASSIGNED")
                .messageText("Test message")
                .sentAt(LocalDateTime.now())
                .success(true)
                .build();

        when(teamsService.getNotificationHistory(1L)).thenReturn(List.of(history));

        // When/Then
        mockMvc.perform(get("/api/teams/configurations/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationType").value("TASK_ASSIGNED"));
    }
}
