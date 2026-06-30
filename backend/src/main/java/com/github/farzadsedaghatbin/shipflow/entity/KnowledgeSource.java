package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.envers.Audited;

/**
 * A configured external content source ingested into the Knowledge Center.
 *
 * <p>Single-org deployment: no organization FK. ORG scope means visible to all
 * authenticated users.
 */
@Entity
@Table(name = "knowledge_sources")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeSource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider_type", nullable = false, length = 32)
  private KnowledgeProviderType providerType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private KnowledgeSourceScope scope;

  @Column(name = "team_id")
  private Long teamId;

  @Column(name = "project_id")
  private Long projectId;

  /** Provider-specific configuration serialized as JSON text. */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String config;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private KnowledgeSourceStatus status;

  @Column(name = "last_ingested_at")
  private OffsetDateTime lastIngestedAt;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

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
    if (status == null) {
      status = KnowledgeSourceStatus.PENDING;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
