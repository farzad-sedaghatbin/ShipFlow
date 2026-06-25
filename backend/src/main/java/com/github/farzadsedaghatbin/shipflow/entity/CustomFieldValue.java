package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * Stores a single encoded value for one custom field definition on one entity instance.
 *
 * <p>Intentionally not {@code @Audited}: values change frequently; audit history of the parent
 * entity (task/pitch/bug) carries sufficient change tracking via Hibernate Envers.
 */
@Entity
@Table(
    name = "custom_field_values",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_cfv_def_entity",
            columnNames = {"definition_id", "entity_type", "entity_id"}),
    indexes = {
      @Index(name = "idx_cfv_definition", columnList = "definition_id"),
      @Index(name = "idx_cfv_entity", columnList = "entity_type, entity_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldValue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "definition_id", nullable = false)
  private CustomFieldDefinition definition;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 50)
  private CustomFieldEntityType entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  /** Encoded string value; encoding depends on field type (see CustomFieldService). */
  @Column(columnDefinition = "TEXT")
  private String value;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_id")
  private User updatedBy;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  @PreUpdate
  protected void onSave() {
    updatedAt = OffsetDateTime.now();
  }
}
