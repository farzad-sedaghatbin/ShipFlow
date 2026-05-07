package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Anthropic Claude LLM provider implementation.
 *
 * <p>Supports Claude 4.x generation models (check available models for your API key at
 * {@code GET https://api.anthropic.com/v1/models}):
 * <ul>
 *   <li>claude-haiku-4-5-20251001 — cheapest/fastest, default, good for all ShipFlow AI features
 *   <li>claude-sonnet-4-5-20250929 — better quality, recommended for Wise Architecture &amp; risk analysis
 *   <li>claude-sonnet-4-6 — latest Sonnet, best quality/cost balance
 *   <li>claude-opus-4-6 — highest capability, premium pricing
 * </ul>
 *
 * <p>Note: older Claude 3.x model IDs (claude-3-5-haiku-20241022, claude-3-haiku-20240307, etc.)
 * are NOT available on newer API keys — those keys only expose the Claude 4.x generation.
 *
 * <p>Configure via:
 * <pre>
 *   app.ai.provider=anthropic
 *   app.ai.anthropic.api-key=${APP_AI_ANTHROPIC_API_KEY}
 *   app.ai.anthropic.model=${APP_AI_ANTHROPIC_MODEL:claude-haiku-4-5-20251001}
 * </pre>
 */
@Component
@Slf4j
public class AnthropicLLMProvider implements LLMProvider {

  private static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";

  @Override
  public LLMProviderType getProviderType() {
    return LLMProviderType.ANTHROPIC;
  }

  @Override
  public ChatLanguageModel createModel(LLMProviderConfig config) {
    validateConfig(config);

    String modelName = (config.getModelName() != null && !config.getModelName().trim().isEmpty())
        ? config.getModelName()
        : DEFAULT_MODEL;

    log.info("Creating Anthropic ChatLanguageModel - Model: {}", modelName);

    AnthropicChatModel.AnthropicChatModelBuilder builder =
        AnthropicChatModel.builder().apiKey(config.getApiKey()).modelName(modelName);

    if (config.getTimeout() != null) {
      builder.timeout(config.getTimeout());
    }

    if (config.getTemperature() != null) {
      builder.temperature(config.getTemperature());
    }

    if (config.getMaxTokens() != null) {
      builder.maxTokens(config.getMaxTokens());
    }

    Boolean logRequests = config.getExtraParam("logRequests", false);
    Boolean logResponses = config.getExtraParam("logResponses", false);
    builder.logRequests(logRequests);
    builder.logResponses(logResponses);

    return builder.build();
  }

  @Override
  public void validateConfig(LLMProviderConfig config) {
    if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Anthropic provider requires an API key (app.ai.anthropic.api-key). "
              + "Get one at https://console.anthropic.com/settings/keys");
    }
  }

  @Override
  public boolean requiresApiKey() {
    return true;
  }

  @Override
  public boolean requiresBaseUrl() {
    return false;
  }
}
