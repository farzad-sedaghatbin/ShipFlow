package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Vector Store Provider implementation for In-Memory storage.
 *
 * <p>
 * In-Memory store is ideal for:
 *
 * <ul>
 * <li>Local development without external dependencies
 * <li>Testing and CI/CD pipelines
 * <li>Quick prototyping
 * <li>Small datasets that fit in memory
 * </ul>
 *
 * <p>
 * <b>Important:</b> Data is NOT persisted. All embeddings are lost on
 * application restart.
 *
 * <p>
 * Configuration: No configuration required.
 */
@Component
@Slf4j
public class InMemoryVectorStoreProvider implements VectorStoreProvider {

  @Override
  public VectorStoreProviderType getProviderType() {
    return VectorStoreProviderType.IN_MEMORY;
  }

  @Override
  public EmbeddingStore<TextSegment> createStore(VectorStoreProviderConfig config) {
    log.info("Creating In-Memory embedding store (dimension: {})", config.getDimension());
    log.warn("In-Memory store is NOT persistent - data will be lost on restart!");
    return new InMemoryEmbeddingStore<>();
  }

  @Override
  public void validateConfig(VectorStoreProviderConfig config) {
    // No validation required - in-memory store has no external dependencies
    log.debug("Validating In-Memory configuration - no requirements");
  }

  @Override
  public boolean requiresApiKey() {
    return false;
  }

  @Override
  public boolean requiresUrl() {
    return false;
  }

  @Override
  public int getDefaultPort() {
    return 0; // Not applicable
  }

  @Override
  public String getDescription() {
    return "In-Memory (Development/Testing) - Non-persistent, no external dependencies";
  }
}
