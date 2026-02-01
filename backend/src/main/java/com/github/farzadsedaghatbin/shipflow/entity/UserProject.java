package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Junction entity for direct user-project assignments. Users can access projects through: 1. Being
 * the project owner 2. Team membership (via TeamAssignment → Team → Cycle → Project) 3. Direct
 * assignment via this entity
 *
 * <p>ADMINs have global access and don't need explicit assignment.
 */
@Entity
@Table(
    name = "user_projects",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_user_project",
            columnNames = {"user_id", "project_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProject {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  /**
   * The role this user has within this specific project. VIEWER: Read-only access to project data
   * CONTRIBUTOR: Can create/edit pitches, tasks, bugs within project MANAGER: Full project access
   * including team management
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ProjectRole projectRole = ProjectRole.VIEWER;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "granted_by_user_id")
  private User grantedBy;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
