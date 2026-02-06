package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.slack.*;
import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackNotificationHistory;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.slack.SlackIntegrationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for SlackIntegrationController without Spring context
 * Pure unit tests using Mockito and standalone MockMvc
 */
@ExtendWith(MockitoExtension.class)
class SlackIntegrationControllerUnitTest {

  @Mock
  private SlackIntegrationService slackService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private SlackIntegrationController controller;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void createConfiguration_WithValidRequest_ShouldReturn201() throws Exception {
    // Given
    CreateSlackConfigurationRequest request = CreateSlackConfigurationRequest.builder()
        .workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL")
        .defaultChannel("general")
        .isEnabled(true)
        .build();

    SlackConfigurationDTO response = SlackConfigurationDTO.builder()
        .id(1L)
        .workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL")
        .defaultChannel("general")
        .isEnabled(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(slackService.createOrUpdateConfiguration(any())).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/slack/configurations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.workspaceName").value("test-workspace"))
        .andExpect(jsonPath("$.isEnabled").value(true));

    verify(slackService).createOrUpdateConfiguration(any());
  }

  @Test
  void getAllConfigurations_ShouldReturnList() throws Exception {
    // Given
    SlackConfigurationDTO config = SlackConfigurationDTO.builder()
        .id(1L)
        .workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL")
        .defaultChannel("general")
        .isEnabled(true)
        .build();

    when(slackService.getAllConfigurations()).thenReturn(List.of(config));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].workspaceName").value("test-workspace"));

    verify(slackService).getAllConfigurations();
  }

  @Test
  void getActiveConfiguration_WhenExists_ShouldReturnConfig() throws Exception {
    // Given
    SlackConfigurationDTO config = SlackConfigurationDTO.builder()
        .id(1L)
        .workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL")
        .defaultChannel("general")
        .isEnabled(true)
        .build();

    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(config));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workspaceName").value("test-workspace"));

    verify(slackService).getActiveConfiguration();
  }

  @Test
  void getActiveConfiguration_WhenNotExists_ShouldReturn404() throws Exception {
    // Given
    when(slackService.getActiveConfiguration()).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/active"))
        .andExpect(status().isNotFound());

    verify(slackService).getActiveConfiguration();
  }

  @Test
  void deleteConfiguration_ShouldReturn204() throws Exception {
    // Given
    doNothing().when(slackService).deleteConfiguration(anyLong());

    // When & Then
    mockMvc.perform(delete("/api/slack/configurations/1"))
        .andExpect(status().isNoContent());

    verify(slackService).deleteConfiguration(1L);
  }

  @Test
  void getNotificationHistory_ShouldReturnList() throws Exception {
    // Given
    SlackNotificationHistory history = SlackNotificationHistory.builder()
        .id(1L)
        .build();

    when(slackService.getNotificationHistory(anyLong())).thenReturn(List.of(history));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/1/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    verify(slackService).getNotificationHistory(1L);
  }

  @Test  
  void getChannelConfigs_ShouldReturnList() throws Exception {
    // Given
    SlackChannelConfigDTO channelConfig = SlackChannelConfigDTO.builder()
        .id(1L)
        .channelName("alerts")
        .channelWebhookUrl("https://hooks.slack.com/services/CHANNEL/URL")
        .build();

    when(slackService.getChannelConfigs(anyLong())).thenReturn(List.of(channelConfig));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/1/channels"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].channelName").value("alerts"));

    verify(slackService).getChannelConfigs(1L);
  }
}
