package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;

public interface KnowledgeSourceProvider {
  KnowledgeProviderType getType();

  void validateConfig(JsonNode config) throws InvalidConfigException;

  default ConnectionStatus testConnection(JsonNode config) {
    return ConnectionStatus.ok();
  }

  IngestionResult ingest(KnowledgeSource source, IngestionContext ctx);

  default boolean supportsRefresh() {
    return false;
  }
}
