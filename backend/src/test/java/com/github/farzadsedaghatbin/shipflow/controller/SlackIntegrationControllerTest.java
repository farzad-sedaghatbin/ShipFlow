package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.slack.*;
import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackNotificationHistory;
import com.github.farzadsedaghatbin.shipflow.security.CustomUserDetailsService;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.slack.SlackIntegrationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for SlackIntegrationController Tests REST API endpoints for Slack
 * integration using MockMvc
 */
@WebMvcTest(controllers = SlackIntegrationController.class, 
    excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, 
        OAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    })
class SlackIntegrationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private SlackIntegrationService slackService;

  @MockBean
  private MessageService messageService;

  @MockBean
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private CustomUserDetailsService customUserDetailsService;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createConfiguration_WithValidRequest_ShouldReturn201() throws Exception {
    // Given
    CreateSlackConfigurationRequest request = CreateSlackConfigurationRequest.builder()
        .workspaceName("test-workspace").webhookUrl("https://hooks.slack.com/services/TEST/URL")
        .defaultChannel("general").isEnabled(true).build();

    SlackConfigurationDTO response = SlackConfigurationDTO.builder().id(1L).workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL").defaultChannel("general").isEnabled(true)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    when(slackService.createOrUpdateConfiguration(any())).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/slack/configurations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.workspaceName").value("test-workspace"))
        .andExpect(jsonPath("$.isEnabled").value(true));
  }

  @Test
  @WithMockUser(roles = "USER")
  void createConfiguration_AsNonAdmin_ShouldReturn403() throws Exception {
    // Given
    CreateSlackConfigurationRequest request = CreateSlackConfigurationRequest.builder()
        .workspaceName("test-workspace").webhookUrl("https://hooks.slack.com/services/TEST/URL").build();

    // When & Then
    mockMvc.perform(post("/api/slack/configurations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void getAllConfigurations_ShouldReturnList() throws Exception {
    // Given
    SlackConfigurationDTO config = SlackConfigurationDTO.builder().id(1L).workspaceName("test-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL").isEnabled(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();

    when(slackService.getAllConfigurations()).thenReturn(List.of(config));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].workspaceName").value("test-workspace"));
  }

  @Test
  @WithMockUser
  void getActiveConfiguration_WhenExists_ShouldReturnConfig() throws Exception {
    // Given
    SlackConfigurationDTO config = SlackConfigurationDTO.builder().id(1L).workspaceName("active-workspace")
        .webhookUrl("https://hooks.slack.com/services/TEST/URL").isEnabled(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();

    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(config));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/active")).andExpect(status().isOk())
        .andExpect(jsonPath("$.workspaceName").value("active-workspace"));
  }

  @Test
  @WithMockUser
  void getActiveConfiguration_WhenNotExists_ShouldReturn404() throws Exception {
    // Given
    when(slackService.getActiveConfiguration()).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/active")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteConfiguration_ShouldReturn204() throws Exception {
    // Given
    doNothing().when(slackService).deleteConfiguration(anyLong());

    // When & Then
    mockMvc.perform(delete("/api/slack/configurations/1").with(csrf())).andExpect(status().isNoContent());

    verify(slackService).deleteConfiguration(1L);
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void createChannelConfig_ShouldReturn201() throws Exception {
    // Given
    CreateSlackChannelConfigRequest request = CreateSlackChannelConfigRequest.builder().channelName("engineering")
        .notifyTaskAssigned(true).notifyTaskCompleted(true).build();

    SlackChannelConfigDTO response = SlackChannelConfigDTO.builder().id(1L).slackConfigId(1L)
        .channelName("engineering").notifyTaskAssigned(true).notifyTaskCompleted(true)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    when(slackService.createOrUpdateChannelConfig(anyLong(), any())).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/slack/configurations/1/channels").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.channelName").value("engineering"));
  }

  @Test
  @WithMockUser
  void getChannelConfigs_ShouldReturnList() throws Exception {
    // Given
    SlackChannelConfigDTO config = SlackChannelConfigDTO.builder().id(1L).slackConfigId(1L)
        .channelName("engineering").notifyTaskAssigned(true).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();

    when(slackService.getChannelConfigs(anyLong())).thenReturn(List.of(config));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/1/channels")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].channelName").value("engineering"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void sendTestNotification_ShouldReturn200() throws Exception {
    // Given
    TestSlackNotificationRequest request = TestSlackNotificationRequest.builder().message("Test message")
        .channel("general").build();

    doNothing().when(slackService).sendTestNotification(anyLong(), any());
    when(messageService.getMessage("slack.notification.sent")).thenReturn("Test notification sent successfully");

    // When & Then
    mockMvc.perform(post("/api/slack/configurations/1/test").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(content().string("Test notification sent successfully"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void getNotificationHistory_ShouldReturnList() throws Exception {
    // Given
    SlackNotificationHistory history = SlackNotificationHistory.builder().id(1L).channelName("general")
        .notificationType("TASK_ASSIGNED").messageText("Test notification").success(true)
        .sentAt(LocalDateTime.now()).build();

    when(slackService.getNotificationHistory(anyLong())).thenReturn(List.of(history));

    // When & Then
    mockMvc.perform(get("/api/slack/configurations/1/history")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].notificationType").value("TASK_ASSIGNED"));
  }

  @Test
  void getAllConfigurations_WithoutAuth_ShouldReturn401() throws Exception {
    mockMvc.perform(get("/api/slack/configurations")).andExpect(status().isUnauthorized());
  }
}
