package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

/**
 * Enumeration of supported vector store providers. Add new providers here to extend the system.
 *
 * <p>To add a new vector store:
 *
 * <ol>
 *   <li>Add the type to this enum
 *   <li>Create a provider implementation of {@link VectorStoreProvider}
 *   <li>Add the required dependency to pom.xml
 *   <li>Annotate with @Component for auto-discovery
 * </ol>
 */
public enum VectorStoreProviderType {
  IN_MEMORY("in-memory", "In-Memory (Development/Testing)"),
  QDRANT("qdrant", "Qdrant (Production Recommended)"),
  CHROMA("chroma", "ChromaDB"),
  MILVUS("milvus", "Milvus"),
  PINECONE("pinecone", "Pinecone (Managed Cloud)"),
  WEAVIATE("weaviate", "Weaviate");

  private final String configValue;
  private final String displayName;

  VectorStoreProviderType(String configValue, String displayName) {
    this.configValue = configValue;
    this.displayName = displayName;
  }

  public String getConfigValue() {
    return configValue;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Parse provider type from configuration string.
   *
   * @param value the configuration value
   * @return the matching provider type
   * @throws IllegalArgumentException if no matching provider is found
   */
  public static VectorStoreProviderType fromConfigValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Vector store provider type cannot be null or empty");
    }

    String normalizedValue = value.trim().toLowerCase();
    for (VectorStoreProviderType type : values()) {
      if (type.configValue.equals(normalizedValue)) {
        return type;
      }
    }

    // Build helpful error message with available options
    StringBuilder available = new StringBuilder();
    for (VectorStoreProviderType type : values()) {
      if (available.length() > 0) {
        available.append(", ");
      }
      available.append(type.configValue);
    }

    throw new IllegalArgumentException(
        "Unknown vector store provider: '" + value + "'. Available providers: " + available);
  }

  /** Get a formatted string of all available providers for documentation. */
  public static String getAvailableProvidersInfo() {
    StringBuilder sb = new StringBuilder();
    for (VectorStoreProviderType type : values()) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append("  - ").append(type.configValue).append(": ").append(type.displayName);
    }
    return sb.toString();
  }
}
