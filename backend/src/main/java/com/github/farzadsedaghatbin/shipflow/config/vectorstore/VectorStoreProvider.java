package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Interface for vector store provider plugins. Implement this interface to add support for a new
 * vector store.
 *
 * <p>Each provider implementation should:
 *
 * <ul>
 *   <li>Return its supported provider type via {@link #getProviderType()}
 *   <li>Create and return a properly configured EmbeddingStore via {@link
 *       #createStore(VectorStoreProviderConfig)}
 *   <li>Validate that required configuration parameters are present
 * </ul>
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @Component
 * public class MyVectorStoreProvider implements VectorStoreProvider {
 *     @Override
 *     public VectorStoreProviderType getProviderType() {
 *         return VectorStoreProviderType.MY_STORE;
 *     }
 *
 *     @Override
 *     public EmbeddingStore<TextSegment> createStore(VectorStoreProviderConfig config) {
 *         validateConfig(config);
 *         return MyStore.builder()
 *             .url(config.getUrl())
 *             .collectionName(config.getCollectionName())
 *             .build();
 *     }
 * }
 * }</pre>
 */
public interface VectorStoreProvider {

  /**
   * Get the provider type this implementation supports.
   *
   * @return the vector store provider type
   */
  VectorStoreProviderType getProviderType();

  /**
   * Create an EmbeddingStore instance configured with the given settings.
   *
   * @param config the provider configuration
   * @return a configured EmbeddingStore instance
   * @throws IllegalArgumentException if required configuration is missing
   * @throws RuntimeException if the store cannot be created
   */
  EmbeddingStore<TextSegment> createStore(VectorStoreProviderConfig config);

  /**
   * Validate that the configuration contains all required parameters for this provider. Called
   * automatically by the factory before creating the store.
   *
   * @param config the configuration to validate
   * @throws IllegalArgumentException if required configuration is missing
   */
  void validateConfig(VectorStoreProviderConfig config);

  /**
   * Indicates whether this provider requires an API key. Providers can override this to indicate
   * their authentication requirements.
   *
   * @return true if API key is required, false otherwise
   */
  default boolean requiresApiKey() {
    return false;
  }

  /**
   * Indicates whether this provider requires a base URL. Most remote vector stores require a URL.
   *
   * @return true if base URL is required, false otherwise
   */
  default boolean requiresUrl() {
    return true;
  }

  /**
   * Get the default port for this provider.
   *
   * @return the default port number
   */
  default int getDefaultPort() {
    return 6333; // Qdrant default
  }

  /**
   * Get a description of this provider for documentation.
   *
   * @return provider description
   */
  default String getDescription() {
    return getProviderType().getDisplayName();
  }
}
