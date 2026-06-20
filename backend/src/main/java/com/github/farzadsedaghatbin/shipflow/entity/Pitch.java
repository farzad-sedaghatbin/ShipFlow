package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(name = "pitches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Pitch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  /** Appetite in days. Required for SHAPED status and beyond. NULL for IDEA. */
  @NotAudited
  @Column(nullable = true)
  private Integer appetiteDays;

  // Shape Up Methodology Fields
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String problemStatement;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String solution;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String rabbitHoles;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String risks;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String noGos;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String wireframeLinks;

  /** Cycle this pitch is assigned to. Required for PENDING status and beyond. NULL for IDEA, DRAFT, SHAPED. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = true)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "pitches", "teams", "retrospectives", "project"})
  private Cycle cycle;

  /** Direct project association — canonical for pre-cycle pitches and for admin move-to-project. */
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = true)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Project project;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cycle", "members", "pitches"})
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PitchStatus status;

  // Circuit Breaker - Shape Up safety valve for overflow
  @Builder.Default
  @Column(nullable = false)
  private Boolean isCircuitBreakerTriggered = false;

  @Column(columnDefinition = "TEXT")
  private String circuitBreakerReason;

  @NotAudited
  @Column
  private LocalDateTime circuitBreakerDate;

  /** The epic this pitch belongs to (optional, for roadmap organization) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "epic_id")
  private Epic epic;

  /** The target release for this pitch (optional) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_release_id")
  private Release targetRelease;

  /** Business value priority (HIGH, MEDIUM, LOW) for ordering within an epic */
  @NotAudited
  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private BusinessValue priority;

  /** Sort order for display within epic or list views */
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

  @NotAudited
  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<WorkLog> workLogs = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Meeting> meetings = new ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Evidence> evidences = new ArrayList<>();

  // v0.5 - Tag-based correlation for linking to retro items
  @NotAudited
  @Builder.Default
  @ManyToMany
  @JoinTable(
      name = "pitch_tags",
      joinColumns = @JoinColumn(name = "pitch_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    // Default to IDEA status for new pitches without explicit status
    if (status == null) {
      status = PitchStatus.IDEA;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // ===== Helper Methods for Status Workflow =====

  /** Returns true if this pitch is assigned to a cycle. */
  public boolean isAssignedToCycle() {
    return cycle != null;
  }

  /** Returns true if this pitch is in a pre-cycle status (IDEA, DRAFT, SHAPED). */
  public boolean isPreCycle() {
    return status == PitchStatus.IDEA 
        || status == PitchStatus.DRAFT 
        || status == PitchStatus.SHAPED;
  }

  /** Returns true if this pitch requires shaping (IDEA or DRAFT). */
  public boolean requiresShaping() {
    return status == PitchStatus.IDEA || status == PitchStatus.DRAFT;
  }

  /** Returns true if this pitch is ready for betting (SHAPED status). */
  public boolean isReadyForBetting() {
    return status == PitchStatus.SHAPED && cycle == null;
  }

  /** Returns true if this pitch is actively being worked (STARTED to TESTING). */
  public boolean isInProgress() {
    return status == PitchStatus.STARTED 
        || status == PitchStatus.IN_PROGRESS 
        || status == PitchStatus.TESTING;
  }

  /** Returns true if this pitch is complete (DONE, COOLDOWN, CANCELLED, CIRCUIT_BREAKER). */
  public boolean isComplete() {
    return status == PitchStatus.DONE 
        || status == PitchStatus.COOLDOWN
        || status == PitchStatus.CANCELLED 
        || status == PitchStatus.CIRCUIT_BREAKER;
  }
}
