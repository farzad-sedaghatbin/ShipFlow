package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import static org.junit.jupiter.api.Assertions.*;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for AnthropicLLMProvider. */
class AnthropicLLMProviderTest {

  private AnthropicLLMProvider provider;

  @BeforeEach
  void setUp() {
    provider = new AnthropicLLMProvider();
  }

  @Test
  void getProviderType_shouldReturnAnthropic() {
    assertEquals(LLMProviderType.ANTHROPIC, provider.getProviderType());
  }

  @Test
  void getDisplayName_shouldReturnCorrectName() {
    assertEquals("Anthropic Claude", provider.getDisplayName());
  }

  @Test
  void requiresApiKey_shouldReturnTrue() {
    assertTrue(provider.requiresApiKey());
  }

  @Test
  void requiresBaseUrl_shouldReturnFalse() {
    assertFalse(provider.requiresBaseUrl());
  }

  @Test
  void validateConfig_shouldThrowWithoutApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .modelName("claude-3-5-haiku-20241022")
        .build();

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> provider.validateConfig(config));
    assertTrue(ex.getMessage().contains("API key"));
    assertTrue(ex.getMessage().contains("console.anthropic.com"));
  }

  @Test
  void validateConfig_shouldThrowWithEmptyApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("   ").build();

    assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
  }

  @Test
  void validateConfig_shouldPassWithValidApiKey() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .apiKey("sk-ant-test-key")
        .build();

    assertDoesNotThrow(() -> provider.validateConfig(config));
  }

  @Test
  void createModel_shouldReturnNonNullWithMinimalConfig() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .apiKey("sk-ant-test-key")
        .build();

    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldReturnNonNullWithFullConfig() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .apiKey("sk-ant-test-key")
        .modelName("claude-3-5-sonnet-20241022")
        .timeout(Duration.ofSeconds(120))
        .temperature(0.3)
        .maxTokens(2048)
        .build();

    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldFallBackToDefaultModelWhenNameIsNull() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .apiKey("sk-ant-test-key")
        .modelName(null)
        .build();

    // Should not throw — falls back to DEFAULT_MODEL
    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldFallBackToDefaultModelWhenNameIsBlank() {
    LLMProviderConfig config = LLMProviderConfig.builder()
        .apiKey("sk-ant-test-key")
        .modelName("   ")
        .build();

    // Should not throw — blank treated same as absent
    ChatLanguageModel model = provider.createModel(config);
    assertNotNull(model);
  }
}
