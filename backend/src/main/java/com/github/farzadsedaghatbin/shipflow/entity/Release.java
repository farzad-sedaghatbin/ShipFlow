package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity representing a release/milestone in the roadmap.
 * Releases are delivery milestones that can contain items from multiple epics.
 * They can span multiple cycles and serve as cross-cutting delivery targets.
 */
@Entity
@Table(name = "releases", indexes = {
    @Index(name = "idx_release_project", columnList = "project_id"),
    @Index(name = "idx_release_status", columnList = "status"),
    @Index(name = "idx_release_target_date", columnList = "target_date"),
    @Index(name = "idx_release_version", columnList = "version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Release {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Name of the release (e.g., "Q2 2026 Release", "Holiday Feature Drop") */
  @Column(nullable = false, length = 255)
  private String name;

  /** Version string (e.g., "v2.4.0", "2026.Q2") */
  @NotAudited
  @Column(length = 50)
  private String version;

  /** Detailed description of the release goals and contents */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String description;

  /** Current status of the release */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReleaseStatus status;

  /** Risk level indicator for this release */
  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level")
  @Builder.Default
  private ReleaseRiskLevel riskLevel = ReleaseRiskLevel.LOW;

  /** Target release date */
  @NotAudited
  @Column(name = "target_date")
  private LocalDate targetDate;

  /** Actual release date (set when released) */
  @NotAudited
  @Column(name = "release_date")
  private LocalDate releaseDate;

  /** Release notes in markdown format */
  @NotAudited
  @Column(name = "release_notes", columnDefinition = "TEXT")
  private String releaseNotes;

  /** The project this release belongs to */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  /** Cycles linked to this release (many-to-many) */
  @NotAudited
  @ManyToMany
  @JoinTable(
      name = "release_cycles",
      joinColumns = @JoinColumn(name = "release_id"),
      inverseJoinColumns = @JoinColumn(name = "cycle_id")
  )
  @Builder.Default
  private Set<Cycle> cycles = new HashSet<>();

  /** Sort order for display in release lists */
  @NotAudited
  @Column(name = "sort_order")
  @Builder.Default
  private Integer sortOrder = 0;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Soft delete fields
  @NotAudited
  @Column
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = ReleaseStatus.PLANNING;
    }
    if (riskLevel == null) {
      riskLevel = ReleaseRiskLevel.LOW;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Helper methods for cycle management
  public void addCycle(Cycle cycle) {
    cycles.add(cycle);
  }

  public void removeCycle(Cycle cycle) {
    cycles.remove(cycle);
  }
}
