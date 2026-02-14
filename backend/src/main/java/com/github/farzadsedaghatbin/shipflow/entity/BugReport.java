package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * Entity representing a bug report in the QA system. Bug reports can be linked
 * to pitches, cycles, test runs, and assignees.
 */
@Entity
@Table(name = "bug_reports", indexes = {@Index(name = "idx_bug_report_project", columnList = "project_id"),
    @Index(name = "idx_bug_report_pitch", columnList = "pitch_id"),
    @Index(name = "idx_bug_report_cycle", columnList = "cycle_id"),
    @Index(name = "idx_bug_report_status", columnList = "status"),
    @Index(name = "idx_bug_report_severity", columnList = "severity"),
    @Index(name = "idx_bug_report_assignee", columnList = "assignee_id"),
    @Index(name = "idx_bug_report_reporter", columnList = "reporter_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class BugReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique identifier for the bug report (e.g., BUG-001). */
  @NotAudited
  @Column(nullable = false, unique = true, length = 50)
  private String bugKey;

  /** Title/summary of the bug. */
  @Column(nullable = false, length = 255)
  private String title;

  /** Detailed description of the bug in Markdown format. */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

  /** Steps to reproduce the bug. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String stepsToReproduce;

  /** Expected behavior. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String expectedBehavior;

  /** Actual behavior observed. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String actualBehavior;

  /** Environment details (browser, OS, version, etc.). */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String environment;

  /**
   * The project this bug belongs to. Direct project association allows bugs to
   * exist without requiring a cycle, pitch, or task - useful for Kanban projects
   * and general/smoke testing.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  /** The pitch this bug is associated with (optional, Shape Up specific). */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /** The cycle this bug was found in. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private Cycle cycle;

  /** The team responsible for fixing this bug. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  /** The test run that discovered this bug (if any). */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_run_id")
  private TestRun testRun;

  /**
   * The scope (hill chart point) this bug is related to (optional). Links the bug
   * to a specific scope for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scope_id")
  private HillChartPoint scope;

  /**
   * The task this bug is related to (optional). Links the bug to a specific task
   * for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  /** Severity of the bug. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BugSeverity severity;

  /** Current status of the bug. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BugStatus status;

  /** Tags for categorization (comma-separated). */
  @NotAudited
  @Column(length = 500)
  private String tags;

  /** URL or path to screenshots/attachments. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String attachments;

  /** User who reported this bug. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  /** Person assigned to fix this bug. */
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private Person assignee;

  /** Resolution notes. */
  @Column(columnDefinition = "TEXT")
  private String resolution;

  /** Date when the bug was resolved. */
  @NotAudited
  private LocalDateTime resolvedAt;

  /** The target release where this bug is expected to be fixed (optional) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_release_id")
  private Release targetRelease;

  /** The release where this bug was actually fixed (may differ from target) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fixed_in_release_id")
  private Release fixedInRelease;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
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
