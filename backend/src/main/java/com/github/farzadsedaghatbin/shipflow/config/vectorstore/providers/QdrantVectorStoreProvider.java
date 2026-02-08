package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Vector Store Provider implementation for Qdrant.
 *
 * <p>
 * Qdrant is the recommended production vector database:
 *
 * <ul>
 * <li>High performance (written in Rust)
 * <li>Excellent filtered search capabilities
 * <li>Horizontal scaling support
 * <li>Built-in clustering and replication
 * <li>Snapshot and backup features
 * <li>REST and gRPC APIs
 * </ul>
 *
 * <p>
 * Configuration:
 *
 * <ul>
 * <li>host: Qdrant server host (default: localhost)
 * <li>port: Qdrant gRPC port (default: 6334)
 * <li>apiKey: API key for authentication (required in production)
 * <li>collectionName: Name of the collection (default: shipflow_knowledge)
 * <li>dimension: Vector dimension (default: 384 for all-MiniLM-L6-v2)
 * </ul>
 */
@Component
@Slf4j
public class QdrantVectorStoreProvider implements VectorStoreProvider {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_GRPC_PORT = 6334;

  @Override
  public VectorStoreProviderType getProviderType() {
    return VectorStoreProviderType.QDRANT;
  }

  @Override
  public EmbeddingStore<TextSegment> createStore(VectorStoreProviderConfig config) {
    validateConfig(config);

    String host = config.getHost() != null ? config.getHost() : DEFAULT_HOST;
    int port = config.getPort() > 0 ? config.getPort() : DEFAULT_GRPC_PORT;
    String collectionName = config.getCollectionName();
    int dimension = config.getDimension();

    log.info("Creating Qdrant embedding store - Host: {}, Port: {}, Collection: {}, Dimension: {}", host, port,
        collectionName, dimension);

    QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder().host(host).port(port)
        .collectionName(collectionName);

    // Configure API key if provided
    if (config.hasApiKey()) {
      log.info("Qdrant authentication enabled with API key");
      builder.apiKey(config.getApiKey());
    } else {
      log.warn("Qdrant running without API key authentication - NOT recommended for production");
    }

    // Handle extra parameters
    Boolean useTls = config.getExtraParam("useTls", false);
    if (useTls) {
      builder.useTls(true);
      log.info("Qdrant TLS enabled");
    }

    return builder.build();
  }

  @Override
  public void validateConfig(VectorStoreProviderConfig config) {
    if (config.getCollectionName() == null || config.getCollectionName().trim().isEmpty()) {
      throw new IllegalArgumentException("Qdrant collection name is required");
    }

    if (config.getDimension() <= 0) {
      throw new IllegalArgumentException("Vector dimension must be positive");
    }

    // Warn if no API key in what appears to be a production setup
    if (!config.hasApiKey()) {
      log.warn("No API key configured for Qdrant. Strongly recommended for production use.");
    }

    log.debug("Validated Qdrant configuration: {}", config);
  }

  @Override
  public boolean requiresApiKey() {
    return true; // Recommended for production
  }

  @Override
  public boolean requiresUrl() {
    return true;
  }

  @Override
  public int getDefaultPort() {
    return DEFAULT_GRPC_PORT;
  }

  @Override
  public String getDescription() {
    return "Qdrant (Production Recommended) - High-performance vector database with filtering, "
        + "clustering, and enterprise features";
  }
}
