package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import static org.junit.jupiter.api.Assertions.*;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for OpenAILLMProvider. */
class OpenAILLMProviderTest {

  private OpenAILLMProvider provider;

  @BeforeEach
  void setUp() {
    provider = new OpenAILLMProvider();
  }

  @Test
  void getProviderType_shouldReturnOpenAI() {
    assertEquals(LLMProviderType.OPENAI, provider.getProviderType());
  }

  @Test
  void requiresApiKey_shouldReturnTrue() {
    assertTrue(provider.requiresApiKey());
  }

  @Test
  void requiresBaseUrl_shouldReturnFalse() {
    // Base URL is optional for OpenAI (has default)
    assertFalse(provider.requiresBaseUrl());
  }

  @Test
  void getDisplayName_shouldReturnCorrectName() {
    assertEquals("OpenAI ChatGPT", provider.getDisplayName());
  }

  @Test
  void createModel_shouldCreateWithFullConfig() {
    LLMProviderConfig config =
        LLMProviderConfig.builder()
            .apiKey("sk-test-key-12345")
            .modelName("gpt-4o-mini")
            .timeout(Duration.ofSeconds(120))
            .temperature(0.3)
            .maxTokens(2048)
            .extraParam("organizationId", "org-123")
            .extraParam("topP", 0.9)
            .build();

    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldUseDefaults() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("sk-test-key-12345").build();

    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldSupportCustomBaseUrl() {
    LLMProviderConfig config =
        LLMProviderConfig.builder()
            .apiKey("sk-test-key-12345")
            .baseUrl("https://custom-openai-proxy.com/v1")
            .modelName("gpt-4o")
            .build();

    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void validateConfig_shouldThrowWithoutApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder().modelName("gpt-4o-mini").build();

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
    assertTrue(ex.getMessage().contains("API key"));
  }

  @Test
  void validateConfig_shouldThrowWithEmptyApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("   ").build();

    assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
  }

  @Test
  void validateConfig_shouldPassWithValidApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("sk-test-key-12345").build();

    assertDoesNotThrow(() -> provider.validateConfig(config));
  }
}
