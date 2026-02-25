package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
  private static final int QDRANT_TIMEOUT_SECONDS = 30;

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

    Boolean useTls = config.getExtraParam("useTls", false);
    ensureCollectionExists(host, port, useTls, collectionName, dimension, config.hasApiKey() ? config.getApiKey() : null);

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
    if (useTls) {
      builder.useTls(true);
      log.info("Qdrant TLS enabled");
    }

    return builder.build();
  }

  /**
   * Ensures the Qdrant collection exists, creating it with cosine distance
   * if it does not. Uses a short-lived QdrantClient for the check/create,
   * then closes it — the EmbeddingStore manages its own connection pool.
   */
  private void ensureCollectionExists(String host, int port, boolean useTls, String collectionName, int dimension, String apiKey) {
    QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(host, port, useTls);
    if (apiKey != null && !apiKey.isBlank()) {
      grpcBuilder.withApiKey(apiKey);
    }

    try (QdrantClient qdrantClient = new QdrantClient(grpcBuilder.build())) {
      boolean exists = qdrantClient.listCollectionsAsync()
          .get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .contains(collectionName);
      if (exists) {
        log.info("Qdrant collection '{}' already exists", collectionName);
        return;
      }

      log.info("Qdrant collection '{}' not found — creating with dimension {} and Cosine distance",
          collectionName, dimension);

      qdrantClient.createCollectionAsync(
          collectionName,
          VectorParams.newBuilder()
              .setSize(dimension)
              .setDistance(Distance.Cosine)
              .build()
      ).get(QDRANT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      log.info("Qdrant collection '{}' created successfully", collectionName);

    } catch (TimeoutException e) {
      throw new IllegalStateException(
          "Timed out after " + QDRANT_TIMEOUT_SECONDS + "s connecting to Qdrant at " + host + ":" + port
              + " while ensuring collection '" + collectionName + "' exists", e);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to ensure Qdrant collection '" + collectionName + "' exists on " + host + ":" + port
              + ": " + e.getMessage(), e);
    }
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
