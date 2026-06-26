package com.github.farzadsedaghatbin.shipflow.service.storage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Registry of all {@link ObjectStorageProvider} beans — mirrors {@code KnowledgeSourceRegistry}.
 *
 * <p>Spring auto-collects every {@code ObjectStorageProvider} bean in the application context and
 * makes them available by {@link StorageProviderType}.
 */
@Component
public class ObjectStorageRegistry {

  private final Map<StorageProviderType, ObjectStorageProvider> byType;

  public ObjectStorageRegistry(List<ObjectStorageProvider> providers) {
    this.byType =
        providers.stream()
            .collect(Collectors.toMap(ObjectStorageProvider::getType, p -> p, (a, b) -> a));
  }

  public ObjectStorageProvider get(StorageProviderType type) {
    var p = byType.get(type);
    if (p == null) throw new IllegalStateException("No storage provider registered for " + type);
    return p;
  }

  public boolean isAvailable(StorageProviderType type) {
    return byType.containsKey(type);
  }
}
