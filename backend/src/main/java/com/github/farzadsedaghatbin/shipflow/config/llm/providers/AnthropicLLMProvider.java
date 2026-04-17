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
 * <p>Supports the full Claude 3.x and Claude 3.5 model family:
 * <ul>
 *   <li>claude-3-5-sonnet-20241022 — best quality, recommended for Wise Architecture &amp; risk analysis
 *   <li>claude-3-5-haiku-20241022 — cost-efficient default, good for all ShipFlow AI features
 *   <li>claude-3-opus-20240229 — highest capability, premium pricing
 * </ul>
 *
 * <p>Configure via:
 * <pre>
 *   app.ai.provider=anthropic
 *   app.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
 *   app.ai.anthropic.model=${ANTHROPIC_MODEL:claude-3-5-haiku-20241022}
 * </pre>
 */
@Component
@Slf4j
public class AnthropicLLMProvider implements LLMProvider {

  private static final String DEFAULT_MODEL = "claude-3-5-haiku-20241022";

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
          "Anthropic provider requires an API key. "
              + "Set ANTHROPIC_API_KEY or APP_AI_ANTHROPIC_API_KEY environment variable. "
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
