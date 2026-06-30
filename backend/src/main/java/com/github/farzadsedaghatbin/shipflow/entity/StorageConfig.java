package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * Singleton row that holds the active object-storage backend and its JSON configuration.
 *
 * <p>Single-row table — no organization FK, mirrors the singleton style of OrganizationSettings.
 * Soft-deleted via {@code deletedAt}; active row retrieved with
 * {@code findFirstByDeletedAtIsNullOrderByIdAsc()}.
 */
@Entity
@Table(name = "storage_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "active_provider", nullable = false, length = 16)
  @Builder.Default
  private StorageProviderType activeProvider = StorageProviderType.LOCAL_FS;

  /** Provider-specific configuration serialized as JSON text. */
  @Column(columnDefinition = "TEXT", nullable = false)
  @Builder.Default
  private String config = "{}";

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
