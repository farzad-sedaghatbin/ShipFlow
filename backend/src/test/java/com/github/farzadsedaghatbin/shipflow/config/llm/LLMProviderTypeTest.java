package com.github.farzadsedaghatbin.shipflow.config.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for LLMProviderType enum. */
class LLMProviderTypeTest {

  @Test
  void fromConfigValue_shouldReturnCorrectType() {
    assertEquals(LLMProviderType.OLLAMA, LLMProviderType.fromConfigValue("ollama"));
    assertEquals(LLMProviderType.RUNPOD, LLMProviderType.fromConfigValue("runpod"));
    assertEquals(LLMProviderType.OPENAI, LLMProviderType.fromConfigValue("openai"));
    assertEquals(LLMProviderType.ANTHROPIC, LLMProviderType.fromConfigValue("anthropic"));
    assertEquals(LLMProviderType.GOOGLE, LLMProviderType.fromConfigValue("google"));
    assertEquals(LLMProviderType.AZURE_OPENAI, LLMProviderType.fromConfigValue("azure-openai"));
  }

  @Test
  void fromConfigValue_shouldBeCaseInsensitive() {
    assertEquals(LLMProviderType.OLLAMA, LLMProviderType.fromConfigValue("OLLAMA"));
    assertEquals(LLMProviderType.OPENAI, LLMProviderType.fromConfigValue("OpenAI"));
    assertEquals(LLMProviderType.RUNPOD, LLMProviderType.fromConfigValue("RunPod"));
  }

  @Test
  void fromConfigValue_shouldHandleWhitespace() {
    assertEquals(LLMProviderType.OLLAMA, LLMProviderType.fromConfigValue("  ollama  "));
    assertEquals(LLMProviderType.OPENAI, LLMProviderType.fromConfigValue(" openai "));
  }

  @Test
  void fromConfigValue_shouldThrowExceptionForNullOrEmpty() {
    assertThrows(IllegalArgumentException.class, () -> LLMProviderType.fromConfigValue(null));
    assertThrows(IllegalArgumentException.class, () -> LLMProviderType.fromConfigValue(""));
    assertThrows(IllegalArgumentException.class, () -> LLMProviderType.fromConfigValue("   "));
  }

  @Test
  void fromConfigValue_shouldThrowExceptionForUnknownProvider() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> LLMProviderType.fromConfigValue("unknown-provider"));
    assertTrue(ex.getMessage().contains("Unknown LLM provider"));
    assertTrue(ex.getMessage().contains("ollama"));
  }

  @Test
  void getSupportedProviders_shouldReturnAllProviders() {
    String supported = LLMProviderType.getSupportedProviders();
    assertTrue(supported.contains("ollama"));
    assertTrue(supported.contains("runpod"));
    assertTrue(supported.contains("openai"));
    assertTrue(supported.contains("anthropic"));
    assertTrue(supported.contains("google"));
    assertTrue(supported.contains("azure-openai"));
  }

  @Test
  void getConfigValue_shouldReturnCorrectValue() {
    assertEquals("ollama", LLMProviderType.OLLAMA.getConfigValue());
    assertEquals("runpod", LLMProviderType.RUNPOD.getConfigValue());
    assertEquals("openai", LLMProviderType.OPENAI.getConfigValue());
  }

  @Test
  void getDisplayName_shouldReturnCorrectName() {
    assertEquals("Ollama (Local/Self-hosted)", LLMProviderType.OLLAMA.getDisplayName());
    assertEquals("OpenAI ChatGPT", LLMProviderType.OPENAI.getDisplayName());
    assertEquals("RunPod Serverless GPU", LLMProviderType.RUNPOD.getDisplayName());
  }
}
