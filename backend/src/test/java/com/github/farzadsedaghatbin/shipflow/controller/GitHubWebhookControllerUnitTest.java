package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.github.GitHubWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookControllerUnitTest {

    @Mock
    private GitHubWebhookService webhookService;

    @InjectMocks
    private GitHubWebhookController gitHubWebhookController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gitHubWebhookController).build();
    }

    @Test
    void handleWebhook_WithoutHeaders_ShouldReturn400() throws Exception {
        // Given
        String payload = "{\"action\":\"opened\"}";

        // When & Then - Missing required headers cause 400
        mockMvc.perform(post("/api/github/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void webhookEndpoint_ShouldExist() throws Exception {
        // When & Then - Test that the webhook endpoint is mapped
        mockMvc.perform(post("/api/github/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest()); // Expected due to missing headers
    }
}