package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity representing a strategic initiative in the roadmap.
 * Initiatives are high-level themes that span multiple quarters and contain epics.
 * Example: "Mobile Experience 2026", "Platform Modernization"
 */
@Entity
@Table(name = "initiatives", indexes = {
    @Index(name = "idx_initiative_project", columnList = "project_id"),
    @Index(name = "idx_initiative_status", columnList = "status"),
    @Index(name = "idx_initiative_owner", columnList = "owner_id"),
    @Index(name = "idx_initiative_target_dates", columnList = "target_start_date, target_end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Initiative {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Name of the initiative (e.g., "Mobile Experience 2026") */
  @Column(nullable = false, length = 255)
  private String name;

  /** Detailed description of the initiative goals and scope */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String description;

  /** Current status of the initiative */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InitiativeStatus status;

  /** Color for timeline/roadmap visualization (hex code) */
  @NotAudited
  @Column(length = 7)
  private String color;

  /** Target start date for the initiative */
  @NotAudited
  @Column(name = "target_start_date")
  private LocalDate targetStartDate;

  /** Target end date for the initiative */
  @NotAudited
  @Column(name = "target_end_date")
  private LocalDate targetEndDate;

  /** The project this initiative belongs to */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  /** User who owns/leads this initiative */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  /** Epics that belong to this initiative */
  @NotAudited
  @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL)
  @Builder.Default
  @org.hibernate.annotations.BatchSize(size = 25)
  private List<Epic> epics = new ArrayList<>();

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
      status = InitiativeStatus.DRAFT;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
