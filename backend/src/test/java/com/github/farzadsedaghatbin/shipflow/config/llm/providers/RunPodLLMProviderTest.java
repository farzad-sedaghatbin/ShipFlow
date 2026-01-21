package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RunPodLLMProvider.
 */
class RunPodLLMProviderTest {

    private RunPodLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RunPodLLMProvider();
    }

    @Test
    void getProviderType_shouldReturnRunPod() {
        assertEquals(LLMProviderType.RUNPOD, provider.getProviderType());
    }

    @Test
    void requiresApiKey_shouldReturnTrue() {
        assertTrue(provider.requiresApiKey());
    }

    @Test
    void requiresBaseUrl_shouldReturnTrue() {
        assertTrue(provider.requiresBaseUrl());
    }

    @Test
    void getDisplayName_shouldReturnCorrectName() {
        assertEquals("RunPod Serverless GPU", provider.getDisplayName());
    }

    @Test
    void createModel_shouldCreateWithFullConfig() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .baseUrl("https://api.runpod.ai/v2/test-endpoint")
                .apiKey("test-api-key")
                .modelName("mistral:instruct")
                .timeout(Duration.ofSeconds(180))
                .temperature(0.3)
                .maxTokens(2048)
                .extraParam("pollInterval", Duration.ofSeconds(2))
                .build();

        ChatLanguageModel model = provider.createModel(config);
        assertNotNull(model);
    }

    @Test
    void validateConfig_shouldThrowWithoutBaseUrl() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .apiKey("test-key")
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> provider.validateConfig(config)
        );
        assertTrue(ex.getMessage().contains("base URL"));
    }

    @Test
    void validateConfig_shouldThrowWithoutApiKey() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .baseUrl("https://api.runpod.ai")
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> provider.validateConfig(config)
        );
        assertTrue(ex.getMessage().contains("API key"));
    }

    @Test
    void validateConfig_shouldPassWithBothRequired() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .baseUrl("https://api.runpod.ai")
                .apiKey("test-key")
                .build();

        assertDoesNotThrow(() -> provider.validateConfig(config));
    }
}
