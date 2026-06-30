package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies {@link StorageConfigRepository} entity mapping and the singleton query.
 *
 * <p>Test profile uses H2 with create-drop; Flyway is disabled. The schema is derived from
 * entity mappings, so this exercises the JPA annotations rather than the SQL migration.
 */
@DataJpaTest
@ActiveProfiles("test")
class StorageConfigRepositoryTest {

  @Autowired private StorageConfigRepository repository;

  @Test
  void saveAndFindFirst_returnsPersistedConfig() {
    StorageConfig cfg =
        StorageConfig.builder()
            .activeProvider(StorageProviderType.LOCAL_FS)
            .config("{}")
            .build();

    StorageConfig saved = repository.save(cfg);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<StorageConfig> found = repository.findFirstByDeletedAtIsNullOrderByIdAsc();
    assertThat(found).isPresent();
    assertThat(found.get().getActiveProvider()).isEqualTo(StorageProviderType.LOCAL_FS);
    assertThat(found.get().getConfig()).isEqualTo("{}");
    assertThat(found.get().getDeletedAt()).isNull();
  }

  @Test
  void findFirst_excludesSoftDeleted() {
    StorageConfig deleted =
        StorageConfig.builder()
            .activeProvider(StorageProviderType.S3)
            .config("{\"bucket\":\"test\"}")
            .build();
    deleted = repository.save(deleted);
    deleted.setDeletedAt(deleted.getUpdatedAt());
    repository.save(deleted);

    Optional<StorageConfig> found = repository.findFirstByDeletedAtIsNullOrderByIdAsc();
    assertThat(found).isEmpty();
  }

  @Test
  void defaultProvider_isLocalFs() {
    StorageConfig cfg = new StorageConfig();
    // activeProvider defaults to LOCAL_FS at field level
    assertThat(cfg.getActiveProvider()).isEqualTo(StorageProviderType.LOCAL_FS);
    assertThat(cfg.getConfig()).isEqualTo("{}");
  }
}
