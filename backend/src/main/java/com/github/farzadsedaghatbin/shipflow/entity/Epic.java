package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity representing an epic in the roadmap.
 * Epics are large features that group related pitches together.
 * Example: "Mobile Checkout Redesign", "Search Performance Optimization"
 */
@Entity
@Table(name = "epics", indexes = {
    @Index(name = "idx_epic_project", columnList = "project_id"),
    @Index(name = "idx_epic_initiative", columnList = "initiative_id"),
    @Index(name = "idx_epic_status", columnList = "status"),
    @Index(name = "idx_epic_owner", columnList = "owner_id"),
    @Index(name = "idx_epic_target_dates", columnList = "target_start_date, target_end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Epic {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Name of the epic (e.g., "Mobile Checkout Redesign") */
  @Column(nullable = false, length = 255)
  private String name;

  /** Detailed description of the epic goals and scope */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String description;

  /** Current status of the epic */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EpicStatus status;

  /** Color for timeline visualization (hex code), inherits from initiative if not set */
  @NotAudited
  @Column(length = 7)
  private String color;

  /** Target start date for the epic */
  @NotAudited
  @Column(name = "target_start_date")
  private LocalDate targetStartDate;

  /** Target end date for the epic */
  @NotAudited
  @Column(name = "target_end_date")
  private LocalDate targetEndDate;

  /** The project this epic belongs to */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  /** The initiative this epic belongs to (optional) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "initiative_id")
  private Initiative initiative;

  /** User who owns/leads this epic */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  /** Pitches that belong to this epic */
  @NotAudited
  @OneToMany(mappedBy = "epic")
  @Builder.Default
  @org.hibernate.annotations.BatchSize(size = 25)
  private List<Pitch> pitches = new ArrayList<>();

  /** Business value priority (HIGH, MEDIUM, LOW) */
  @NotAudited
  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private BusinessValue priority;

  /** Sort order for display in roadmap views */
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
      status = EpicStatus.DRAFT;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
