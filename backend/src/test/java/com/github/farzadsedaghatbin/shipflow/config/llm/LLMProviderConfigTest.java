package com.github.farzadsedaghatbin.shipflow.config.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for LLMProviderConfig builder and configuration container. */
class LLMProviderConfigTest {

  @Test
  void builder_shouldCreateConfigWithAllFields() {
    LLMProviderConfig config = LLMProviderConfig.builder().baseUrl("http://localhost:11434").apiKey("test-key")
        .modelName("test-model").timeout(Duration.ofSeconds(120)).temperature(0.7).maxTokens(1024).build();

    assertEquals("http://localhost:11434", config.getBaseUrl());
    assertEquals("test-key", config.getApiKey());
    assertEquals("test-model", config.getModelName());
    assertEquals(Duration.ofSeconds(120), config.getTimeout());
    assertEquals(0.7, config.getTemperature());
    assertEquals(1024, config.getMaxTokens());
  }

  @Test
  void builder_shouldUseDefaults() {
    LLMProviderConfig config = LLMProviderConfig.builder().build();

    assertNull(config.getBaseUrl());
    assertNull(config.getApiKey());
    assertNull(config.getModelName());
    assertEquals(Duration.ofSeconds(120), config.getTimeout());
    assertEquals(0.3, config.getTemperature());
    assertEquals(2048, config.getMaxTokens());
  }

  @Test
  void extraParams_shouldStoreAndRetrieveValues() {
    LLMProviderConfig config = LLMProviderConfig.builder().extraParam("pollInterval", Duration.ofSeconds(2))
        .extraParam("organizationId", "org-123").extraParam("numCtx", 4096).build();

    assertEquals(Duration.ofSeconds(2), config.getExtraParam("pollInterval", null));
    assertEquals("org-123", config.getExtraParam("organizationId", null));
    assertEquals(Integer.valueOf(4096), config.getExtraParam("numCtx", null));
  }

  @Test
  void extraParams_shouldReturnDefaultForMissingKeys() {
    LLMProviderConfig config = LLMProviderConfig.builder().build();

    assertEquals("default", config.getExtraParam("missing", "default"));
    assertEquals(Integer.valueOf(100), config.getExtraParam("missing", 100));
    assertNull(config.getExtraParam("missing", null));
  }

  @Test
  void extraParams_shouldBeIsolated() {
    LLMProviderConfig config = LLMProviderConfig.builder().extraParam("key", "value").build();

    Map<String, Object> params = config.getExtraParams();
    params.put("newKey", "newValue");

    // Original config should not be affected
    assertNull(config.getExtraParam("newKey", null));
  }

  @Test
  void builder_shouldSupportMultipleExtraParams() {
    Map<String, Object> params = Map.of("param1", "value1", "param2", Integer.valueOf(123), "param3", Boolean.TRUE);

    LLMProviderConfig config = LLMProviderConfig.builder().extraParams(params).build();

    assertEquals("value1", config.getExtraParam("param1", null));
    assertEquals(Integer.valueOf(123), config.getExtraParam("param2", null));
    assertEquals(Boolean.TRUE, config.getExtraParam("param3", null));
  }

  @Test
  void toString_shouldContainKeyInformation() {
    LLMProviderConfig config = LLMProviderConfig.builder().baseUrl("http://test").modelName("test-model")
        .timeout(Duration.ofSeconds(60)).temperature(0.5).maxTokens(512).extraParam("custom", "value").build();

    String str = config.toString();
    assertTrue(str.contains("http://test"), "Should contain baseUrl");
    assertTrue(str.contains("test-model"), "Should contain modelName");
    assertTrue(str.contains("0.5"), "Should contain temperature");
    // Don't check for specific timeout format as it varies
    assertNotNull(str);
  }
}
