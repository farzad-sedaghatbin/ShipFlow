package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OllamaLLMProvider.
 */
class OllamaLLMProviderTest {

    private OllamaLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OllamaLLMProvider();
    }

    @Test
    void getProviderType_shouldReturnOllama() {
        assertEquals(LLMProviderType.OLLAMA, provider.getProviderType());
    }

    @Test
    void requiresApiKey_shouldReturnFalse() {
        assertFalse(provider.requiresApiKey());
    }

    @Test
    void requiresBaseUrl_shouldReturnTrue() {
        assertTrue(provider.requiresBaseUrl());
    }

    @Test
    void getDisplayName_shouldReturnCorrectName() {
        assertEquals("Ollama (Local/Self-hosted)", provider.getDisplayName());
    }

    @Test
    void createModel_shouldCreateWithAllConfig() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .baseUrl("http://localhost:11434")
                .modelName("mistral:instruct")
                .timeout(Duration.ofSeconds(180))
                .temperature(0.3)
                .extraParam("numCtx", 4096)
                .build();

        ChatLanguageModel model = provider.createModel(config);
        assertNotNull(model);
    }

    @Test
    void createModel_shouldUseDefaultsWhenNotProvided() {
        LLMProviderConfig config = LLMProviderConfig.builder().build();

        ChatLanguageModel model = provider.createModel(config);
        assertNotNull(model);
    }

    @Test
    void validateConfig_shouldNotThrowForValidConfig() {
        LLMProviderConfig config = LLMProviderConfig.builder()
                .baseUrl("http://localhost:11434")
                .modelName("mistral:instruct")
                .build();

        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldNotThrowForMinimalConfig() {
        // Ollama doesn't require strict validation - has defaults
        LLMProviderConfig config = LLMProviderConfig.builder().build();
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }
}
