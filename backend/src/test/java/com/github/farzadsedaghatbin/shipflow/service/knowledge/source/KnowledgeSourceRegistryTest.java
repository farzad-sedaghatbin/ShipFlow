package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeSourceRegistryTest {

  static class FakeUrl implements KnowledgeSourceProvider {
    @Override
    public KnowledgeProviderType getType() {
      return KnowledgeProviderType.URL;
    }

    @Override
    public void validateConfig(JsonNode c) {}

    @Override
    public IngestionResult ingest(KnowledgeSource s, IngestionContext c) {
      return null;
    }
  }

  @Test
  void resolves_by_type() {
    var r = new KnowledgeSourceRegistry(List.of(new FakeUrl()));
    assertThat(r.get(KnowledgeProviderType.URL)).isInstanceOf(FakeUrl.class);
    assertThat(r.isAvailable(KnowledgeProviderType.URL)).isTrue();
    assertThat(r.isAvailable(KnowledgeProviderType.GITHUB)).isFalse();
  }

  @Test
  void throws_when_provider_missing() {
    var r = new KnowledgeSourceRegistry(List.of());
    assertThatThrownBy(() -> r.get(KnowledgeProviderType.GITHUB))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GITHUB");
  }
}
