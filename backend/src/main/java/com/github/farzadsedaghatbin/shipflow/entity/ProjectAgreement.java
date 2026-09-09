package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity representing a discrete, dated agreement/contract-term entry logged against a project.
 * Not everything discussed in a meeting belongs on the Task board — some things are agreements
 * ("we agreed X") rather than work items. Each project can accumulate these over time, optionally
 * linked back to the {@link Meeting} they originated from; most agreements are logged directly
 * without a meeting at all.
 */
@Entity
@Table(name = "project_agreements", indexes = {
    @Index(name = "idx_project_agreements_project", columnList = "project_id"),
    @Index(name = "idx_project_agreements_meeting", columnList = "meeting_id"),
    @Index(name = "idx_project_agreements_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class ProjectAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The project this agreement belongs to. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  /** Short title for the agreement. */
  @Column(nullable = false, length = 255)
  private String title;

  /** The agreement text itself. */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  /** The meeting this agreement originated from, if any — most agreements won't have one. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  /** Date the agreement was reached (defaults to today at creation time, in the service layer). */
  @Column(name = "agreed_date", nullable = false)
  private LocalDate agreedDate;

  /** User who logged this agreement. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Soft delete fields
  @NotAudited
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

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
