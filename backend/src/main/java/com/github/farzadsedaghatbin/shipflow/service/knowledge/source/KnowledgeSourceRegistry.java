package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSourceRegistry {
  private final Map<KnowledgeProviderType, KnowledgeSourceProvider> byType;

  public KnowledgeSourceRegistry(List<KnowledgeSourceProvider> providers) {
    this.byType =
        providers.stream()
            .collect(Collectors.toMap(KnowledgeSourceProvider::getType, p -> p, (a, b) -> a));
  }

  public KnowledgeSourceProvider get(KnowledgeProviderType type) {
    var p = byType.get(type);
    if (p == null) throw new IllegalStateException("No provider registered for " + type);
    return p;
  }

  public boolean isAvailable(KnowledgeProviderType type) {
    return byType.containsKey(type);
  }
}
