package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for creating vector store EmbeddingStore instances based on provider
 * type.
 *
 * <p>
 * This factory automatically discovers all registered
 * {@link VectorStoreProvider} implementations and creates the appropriate store
 * based on configuration.
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * @Autowired
 * private VectorStoreProviderFactory factory;
 *
 * EmbeddingStore<TextSegment> store = factory.createStore(VectorStoreProviderType.QDRANT, config);
 * }</pre>
 *
 * <p>
 * To add a new provider:
 *
 * <ol>
 * <li>Add the provider type to {@link VectorStoreProviderType}
 * <li>Create a provider implementation of {@link VectorStoreProvider}
 * <li>Add required dependency to pom.xml
 * <li>Annotate with @Component for auto-discovery
 * </ol>
 */
@Component
@Slf4j
public class VectorStoreProviderFactory {

  private final Map<VectorStoreProviderType, VectorStoreProvider> providers;

  /**
   * Constructor with auto-discovery of all VectorStoreProvider implementations.
   * Spring will inject all beans implementing VectorStoreProvider.
   */
  public VectorStoreProviderFactory(List<VectorStoreProvider> providerList) {
    this.providers = new HashMap<>();

    for (VectorStoreProvider provider : providerList) {
      VectorStoreProviderType type = provider.getProviderType();
      if (providers.containsKey(type)) {
        log.warn("Duplicate vector store provider registration for type {}: {} will be overwritten by {}", type,
            providers.get(type).getClass().getSimpleName(), provider.getClass().getSimpleName());
      }
      providers.put(type, provider);
      log.info("Registered vector store provider: {} -> {}", type.getConfigValue(),
          provider.getClass().getSimpleName());
    }

    log.info("Vector Store Provider Factory initialized with {} providers: {}", providers.size(),
        providers.keySet());
  }

  /**
   * Create an EmbeddingStore for the specified provider type.
   *
   * @param providerType
   *            the type of provider to create
   * @param config
   *            the configuration for the provider
   * @return a configured EmbeddingStore instance
   * @throws IllegalArgumentException
   *             if the provider type is not supported
   * @throws RuntimeException
   *             if store creation fails
   */
  public EmbeddingStore<TextSegment> createStore(VectorStoreProviderType providerType,
      VectorStoreProviderConfig config) {
    VectorStoreProvider provider = providers.get(providerType);

    if (provider == null) {
      throw new IllegalArgumentException("No provider registered for type: " + providerType
          + ". Available providers: " + getAvailableProviders());
    }

    log.info("Creating vector store using provider: {} ({})", providerType.getConfigValue(),
        provider.getClass().getSimpleName());

    // Validate configuration
    try {
      provider.validateConfig(config);
    } catch (Exception e) {
      log.error("Configuration validation failed for provider {}: {}", providerType, e.getMessage());
      throw e;
    }

    // Create the store
    try {
      EmbeddingStore<TextSegment> store = provider.createStore(config);
      log.info("Successfully created vector store: {} with collection '{}'", providerType.getConfigValue(),
          config.getCollectionName());
      return store;
    } catch (Exception e) {
      log.error("Failed to create vector store for provider {}: {}", providerType, e.getMessage(), e);
      throw new RuntimeException("Failed to create vector store: " + e.getMessage(), e);
    }
  }

  /**
   * Create an EmbeddingStore from a provider type string value.
   *
   * @param providerTypeValue
   *            the provider type as a string (e.g., "qdrant", "chroma")
   * @param config
   *            the configuration for the provider
   * @return a configured EmbeddingStore instance
   */
  public EmbeddingStore<TextSegment> createStore(String providerTypeValue, VectorStoreProviderConfig config) {
    VectorStoreProviderType providerType = VectorStoreProviderType.fromConfigValue(providerTypeValue);
    return createStore(providerType, config);
  }

  /** Get the set of available provider types. */
  public Set<VectorStoreProviderType> getAvailableProviders() {
    return providers.keySet();
  }

  /** Get a provider by type. */
  public VectorStoreProvider getProvider(VectorStoreProviderType type) {
    return providers.get(type);
  }

  /** Check if a provider type is available. */
  public boolean isProviderAvailable(VectorStoreProviderType type) {
    return providers.containsKey(type);
  }

  /** Check if a provider type string is available. */
  public boolean isProviderAvailable(String providerTypeValue) {
    try {
      VectorStoreProviderType type = VectorStoreProviderType.fromConfigValue(providerTypeValue);
      return isProviderAvailable(type);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** Get information about all registered providers. */
  public String getProvidersInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Registered Vector Store Providers:\n");

    for (Map.Entry<VectorStoreProviderType, VectorStoreProvider> entry : providers.entrySet()) {
      VectorStoreProviderType type = entry.getKey();
      VectorStoreProvider provider = entry.getValue();
      sb.append(String.format("  - %s (%s): %s%s%n", type.getConfigValue(), type.getDisplayName(),
          provider.getClass().getSimpleName(), provider.requiresApiKey() ? " [API Key Required]" : ""));
    }

    return sb.toString();
  }
}
