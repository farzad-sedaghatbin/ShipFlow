package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Defines a user-created metadata field applicable to Tasks, Pitches, or Bug Reports.
 *
 * <p>Org-wide definitions have {@code project = null} and may only be created by ADMIN.
 * Project-scoped definitions require a project and may be created by MANAGER or ADMIN.
 */
@Entity
@Table(
    name = "custom_field_definitions",
    indexes = {
      @Index(name = "idx_cfd_entity_type", columnList = "entity_type"),
      @Index(name = "idx_cfd_project_id", columnList = "project_id"),
      @Index(name = "idx_cfd_deleted", columnList = "deleted_at")
    })
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 50)
  private CustomFieldType fieldType;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 50)
  private CustomFieldEntityType entityType;

  /** Null means org-wide (ADMIN only). Non-null means project-scoped. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  @Column(nullable = false)
  private Boolean required = false;

  @NotAudited
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  /** JSON array string of allowed values for SELECT / MULTISELECT fields. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String options;

  @NotAudited
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @NotAudited
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @NotAudited
  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
