package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a dependency between two epics. Informational only —
 * no hard reorder enforcement, used for visualising roadmap dependencies.
 */
@Entity
@Table(name = "epic_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = {"source_epic_id",
    "target_epic_id"}), indexes = {@Index(name = "idx_epic_dep_source", columnList = "source_epic_id"),
        @Index(name = "idx_epic_dep_target", columnList = "target_epic_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicDependency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The epic that has the dependency (the blocking epic or the dependent epic).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_epic_id", nullable = false)
  private Epic sourceEpic;

  /**
   * The epic being depended upon (the blocked epic or the epic being waited on).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_epic_id", nullable = false)
  private Epic targetEpic;

  /** Type of dependency relationship. */
  @Enumerated(EnumType.STRING)
  @Column(name = "dependency_type", nullable = false, length = 50)
  @Builder.Default
  private DependencyType dependencyType = DependencyType.BLOCKS;

  /** When this dependency was created. */
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
