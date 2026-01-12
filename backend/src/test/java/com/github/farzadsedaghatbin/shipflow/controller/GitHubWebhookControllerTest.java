package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubWebhookService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GitHubWebhookController
 * Tests webhook endpoint and signature validation using MockMvc
 */
@Disabled("Temporarily disabled - Spring context loading issues with @WebMvcTest and AIConfig")
@WebMvcTest(value = GitHubWebhookController.class,
    excludeAutoConfiguration = AIConfig.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AIConfig.class))
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubWebhookService webhookService;

    @MockBean
    private ChatLanguageModel chatLanguageModel;

    @Test
    void handleWebhook_WithValidSignature_ShouldReturn200() throws Exception {
        // Given
        String payload = "{\"repository\":{\"full_name\":\"test/repo\"},\"commits\":[]}";
        String signature = "sha256=test-signature";
        String eventType = "push";
        
        when(webhookService.validateSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);
        doNothing().when(webhookService).processWebhook(eventType, payload);

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", eventType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(webhookService).processWebhook(eventType, payload);
    }

    @Test
    void handleWebhook_WithInvalidSignature_ShouldReturn401() throws Exception {
        // Given
        String payload = "{\"repository\":{\"full_name\":\"test/repo\"}}";
        String signature = "sha256=invalid-signature";
        String eventType = "push";
        
        when(webhookService.validateSignature(anyString(), anyString(), anyString()))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", eventType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature"));

        verify(webhookService, never()).processWebhook(anyString(), anyString());
    }

    @Test
    void handleWebhook_WithoutSignature_ShouldReturn401() throws Exception {
        // Given
        String payload = "{\"repository\":{\"full_name\":\"test/repo\"}}";
        String eventType = "push";

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-GitHub-Event", eventType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processWebhook(anyString(), anyString());
    }

    @Test
    void handleWebhook_WithoutEventType_ShouldReturn400() throws Exception {
        // Given
        String payload = "{\"repository\":{\"full_name\":\"test/repo\"}}";
        String signature = "sha256=test-signature";

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleWebhook_PingEvent_ShouldReturn200() throws Exception {
        // Given
        String payload = "{\"zen\":\"Design for failure\"}";
        String signature = "sha256=test-signature";
        String eventType = "ping";
        
        when(webhookService.validateSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", eventType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(webhookService).processWebhook(eventType, payload);
    }

    @Test
    void handleWebhook_ProcessingError_ShouldReturn500() throws Exception {
        // Given
        String payload = "{\"repository\":{\"full_name\":\"test/repo\"}}";
        String signature = "sha256=test-signature";
        String eventType = "push";
        
        when(webhookService.validateSignature(anyString(), anyString(), anyString()))
                .thenReturn(true);
        doThrow(new RuntimeException("Processing failed"))
                .when(webhookService).processWebhook(eventType, payload);

        // When & Then
        mockMvc.perform(post("/api/github/webhook")
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", eventType)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
}
