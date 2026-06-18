package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence layer for {@link StorageConfig}.
 *
 * <p>Single-row table: use {@link #findFirstByDeletedAtIsNullOrderByIdAsc()} to retrieve the
 * active configuration row.
 */
@Repository
public interface StorageConfigRepository extends JpaRepository<StorageConfig, Long> {

  Optional<StorageConfig> findFirstByDeletedAtIsNullOrderByIdAsc();
}
