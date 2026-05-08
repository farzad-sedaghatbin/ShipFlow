package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a dependency between two pitches. Supports roadmap-level
 * dependency tracking to enforce ordering constraints during pitch reordering.
 */
@Entity
@Table(name = "pitch_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = {"source_pitch_id",
    "target_pitch_id"}), indexes = {@Index(name = "idx_pitch_dep_source", columnList = "source_pitch_id"),
        @Index(name = "idx_pitch_dep_target", columnList = "target_pitch_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchDependency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The pitch that has the dependency (the blocking pitch or the dependent pitch).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_pitch_id", nullable = false)
  private Pitch sourcePitch;

  /**
   * The pitch being depended upon (the blocked pitch or the pitch being waited on).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_pitch_id", nullable = false)
  private Pitch targetPitch;

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
