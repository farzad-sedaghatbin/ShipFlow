package com.github.farzadsedaghatbin.shipflow.config.llm;

import com.github.farzadsedaghatbin.shipflow.config.AirGappedProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for creating LLM ChatLanguageModel instances based on provider type.
 *
 * <p>
 * This factory automatically discovers all registered {@link LLMProvider}
 * implementations and creates the appropriate model based on configuration.
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * @Autowired
 * private LLMProviderFactory factory;
 *
 * ChatLanguageModel model = factory.createModel(LLMProviderType.OPENAI, config);
 * }</pre>
 *
 * <p>
 * To add a new provider:
 *
 * <ol>
 * <li>Add the provider type to {@link LLMProviderType}
 * <li>Create a provider implementation of {@link LLMProvider}
 * <li>Annotate with @Component for auto-discovery
 * </ol>
 */
@Component
@Slf4j
public class LLMProviderFactory {

  private final Map<LLMProviderType, LLMProvider> providers;
  private final AirGappedProperties airGappedProperties;

  /**
   * Constructor with auto-discovery of all LLMProvider implementations. Spring
   * will inject all beans implementing LLMProvider.
   */
  public LLMProviderFactory(List<LLMProvider> providerList, AirGappedProperties airGappedProperties) {
    this.airGappedProperties = airGappedProperties;
    this.providers = new HashMap<>();

    for (LLMProvider provider : providerList) {
      LLMProviderType type = provider.getProviderType();
      if (providers.containsKey(type)) {
        log.warn("Duplicate LLM provider registration for type {}: {} will be overwritten by {}", type,
            providers.get(type).getClass().getSimpleName(), provider.getClass().getSimpleName());
      }
      providers.put(type, provider);
      log.info("Registered LLM provider: {} -> {}", type.getConfigValue(), provider.getClass().getSimpleName());
    }

    log.info("LLM Provider Factory initialized with {} providers: {}", providers.size(), providers.keySet());
  }

  /**
   * Create a ChatLanguageModel for the specified provider type.
   *
   * @param providerType
   *            the type of provider to create
   * @param config
   *            the configuration for the provider
   * @return a configured ChatLanguageModel instance
   * @throws IllegalArgumentException
   *             if the provider type is not supported
   * @throws RuntimeException
   *             if model creation fails
   */
  public ChatLanguageModel createModel(LLMProviderType providerType, LLMProviderConfig config) {
    // Air-gapped guard: never build a model for a provider that requires external network egress.
    // This stops an admin from switching to a cloud provider at runtime after a boot-time validation.
    if (airGappedProperties.isEnabled() && !providerType.isLocal()) {
      throw new IllegalStateException("Air-gapped mode is enabled but provider '"
          + providerType.getConfigValue()
          + "' requires external network egress. Only local providers (ollama) are permitted. "
          + "Set AI_PROVIDER=ollama or disable air-gapped mode (AIR_GAPPED_MODE=false).");
    }

    LLMProvider provider = providers.get(providerType);

    if (provider == null) {
      throw new IllegalArgumentException("No provider registered for type: " + providerType
          + ". Available providers: " + getAvailableProviders());
    }

    log.info("Creating ChatLanguageModel using provider: {} ({})", providerType.getConfigValue(),
        provider.getDisplayName());

    // Validate configuration before creating model
    provider.validateConfig(config);

    return provider.createModel(config);
  }

  /**
   * Create a ChatLanguageModel using provider type string from configuration.
   *
   * @param providerTypeStr
   *            the provider type as configuration string (e.g., "openai",
   *            "ollama")
   * @param config
   *            the configuration for the provider
   * @return a configured ChatLanguageModel instance
   */
  public ChatLanguageModel createModel(String providerTypeStr, LLMProviderConfig config) {
    LLMProviderType providerType = LLMProviderType.fromConfigValue(providerTypeStr);
    return createModel(providerType, config);
  }

  /**
   * Check if a provider type is available.
   *
   * @param providerType
   *            the type to check
   * @return true if the provider is registered
   */
  public boolean isProviderAvailable(LLMProviderType providerType) {
    return providers.containsKey(providerType);
  }

  /**
   * Check if a provider type string is available.
   *
   * @param providerTypeStr
   *            the type string to check
   * @return true if the provider is registered
   */
  public boolean isProviderAvailable(String providerTypeStr) {
    try {
      LLMProviderType type = LLMProviderType.fromConfigValue(providerTypeStr);
      return isProviderAvailable(type);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Get the set of available provider types.
   *
   * @return set of available provider types
   */
  public Set<LLMProviderType> getAvailableProviders() {
    return providers.keySet();
  }

  /**
   * Get a specific provider instance.
   *
   * @param providerType
   *            the type of provider
   * @return the provider instance, or null if not available
   */
  public LLMProvider getProvider(LLMProviderType providerType) {
    return providers.get(providerType);
  }

  /**
   * Get a human-readable list of available providers and their requirements.
   *
   * @return formatted string describing available providers
   */
  public String getProviderInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Available LLM Providers:\n");

    for (Map.Entry<LLMProviderType, LLMProvider> entry : providers.entrySet()) {
      LLMProvider provider = entry.getValue();
      sb.append(String.format("  - %s (%s)%n", entry.getKey().getConfigValue(), provider.getDisplayName()));
      sb.append(String.format("    Requires API Key: %s%n", provider.requiresApiKey()));
      sb.append(String.format("    Requires Base URL: %s%n", provider.requiresBaseUrl()));
    }

    return sb.toString();
  }
}
