package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a saved filter view (named filter preset) for the task backlog.
 * Scoped per user and project — users can save, load, and set a default view.
 */
@Entity
@Table(
    name = "saved_views",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_saved_view_name_per_user_project",
          columnNames = {"user_id", "project_id", "name"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedView {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /**
   * Filter state serialised as raw JSON. Stored in a JSONB column.
   * Example: {"statusFilter":["BACKLOG"],"priorityFilter":["URGENT"],"sortBy":"createdAt","sortOrder":"desc"}
   */
  @Column(name = "filters", nullable = false, columnDefinition = "jsonb")
  private String filters;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
