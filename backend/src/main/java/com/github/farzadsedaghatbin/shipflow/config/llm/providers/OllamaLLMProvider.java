package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM Provider implementation for Ollama (local/self-hosted).
 *
 * <p>
 * Ollama is ideal for:
 *
 * <ul>
 * <li>Local development without API costs
 * <li>Self-hosted deployments with privacy requirements
 * <li>Running open-source models locally
 * </ul>
 *
 * <p>
 * Configuration:
 *
 * <ul>
 * <li>baseUrl: Ollama API URL (default: http://localhost:11434)
 * <li>modelName: Model to use (e.g., mistral:instruct, llama2, codellama)
 * <li>timeout: Request timeout
 * <li>temperature: Model temperature (0.0-1.0)
 * </ul>
 */
@Component
@Slf4j
public class OllamaLLMProvider implements LLMProvider {

  private static final String DEFAULT_BASE_URL = "http://localhost:11434";
  private static final String DEFAULT_MODEL = "mistral:instruct";

  @Override
  public LLMProviderType getProviderType() {
    return LLMProviderType.OLLAMA;
  }

  @Override
  public ChatLanguageModel createModel(LLMProviderConfig config) {
    validateConfig(config);

    String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
    String modelName = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;

    log.info("Creating Ollama ChatLanguageModel - URL: {}, Model: {}", baseUrl, modelName);

    OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder().baseUrl(baseUrl)
        .modelName(modelName);

    if (config.getTimeout() != null) {
      builder.timeout(config.getTimeout());
    }

    if (config.getTemperature() != null) {
      builder.temperature(config.getTemperature());
    }

    // Handle extra parameters specific to Ollama
    Integer numCtx = config.getExtraParam("numCtx", null);
    if (numCtx != null) {
      builder.numCtx(numCtx);
    }

    return builder.build();
  }

  @Override
  public void validateConfig(LLMProviderConfig config) {
    // Ollama doesn't require API key - it's local
    // Base URL and model name have sensible defaults
    log.debug("Validating Ollama configuration: {}", config);
  }

  @Override
  public boolean requiresApiKey() {
    return false;
  }

  @Override
  public boolean requiresBaseUrl() {
    return true; // Optional but should specify for non-default setups
  }
}
