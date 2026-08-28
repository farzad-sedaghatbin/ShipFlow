package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.providers.AnthropicLLMProvider;
import org.junit.jupiter.api.Test;

/**
 * Guards the Anthropic temperature contract.
 *
 * <p>claude-sonnet-5 and newer reject {@code temperature} outright — the API answers
 * "`temperature` is deprecated for this model" and the whole request fails. ShipFlow applied a
 * shared default of 0.3 to every provider, so simply selecting a current Claude model made every AI
 * feature return a 500. Anthropic therefore takes its own, unset-by-default temperature.
 */
class AnthropicTemperatureConfigTest {

  /** A null temperature must stay null, so the parameter is omitted from the request. */
  @Test
  void temperatureIsOmittedWhenNotConfigured() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("k").modelName("claude-sonnet-5").temperature(null)
        .build();

    assertThat(config.getTemperature()).isNull();
  }

  /** An explicitly configured temperature is still honoured, for older models that accept it. */
  @Test
  void temperatureIsKeptWhenExplicitlyConfigured() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("k").modelName("claude-sonnet-4-6").temperature(0.3)
        .build();

    assertThat(config.getTemperature()).isEqualTo(0.3);
  }

  /** The provider must accept a config carrying no temperature rather than defaulting one in. */
  @Test
  void providerBuildsModelWithoutTemperature() {
    AnthropicLLMProvider provider = new AnthropicLLMProvider();
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("test-key").modelName("claude-sonnet-5")
        .temperature(null).build();

    assertThat(provider.requiresApiKey()).isTrue();
    // Must not throw: a null temperature is the supported, default path.
    assertThat(provider.createModel(config)).isNotNull();
  }
}
