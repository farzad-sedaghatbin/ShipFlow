package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a bug report in the QA system. Bug reports can be linked to pitches, cycles,
 * test runs, and assignees.
 */
@Entity
@Table(
    name = "bug_reports",
    indexes = {
      @Index(name = "idx_bug_report_project", columnList = "project_id"),
      @Index(name = "idx_bug_report_pitch", columnList = "pitch_id"),
      @Index(name = "idx_bug_report_cycle", columnList = "cycle_id"),
      @Index(name = "idx_bug_report_status", columnList = "status"),
      @Index(name = "idx_bug_report_severity", columnList = "severity"),
      @Index(name = "idx_bug_report_assignee", columnList = "assignee_id"),
      @Index(name = "idx_bug_report_reporter", columnList = "reporter_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique identifier for the bug report (e.g., BUG-001). */
  @Column(nullable = false, unique = true, length = 50)
  private String bugKey;

  /** Title/summary of the bug. */
  @Column(nullable = false, length = 255)
  private String title;

  /** Detailed description of the bug in Markdown format. */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

  /** Steps to reproduce the bug. */
  @Column(columnDefinition = "TEXT")
  private String stepsToReproduce;

  /** Expected behavior. */
  @Column(columnDefinition = "TEXT")
  private String expectedBehavior;

  /** Actual behavior observed. */
  @Column(columnDefinition = "TEXT")
  private String actualBehavior;

  /** Environment details (browser, OS, version, etc.). */
  @Column(columnDefinition = "TEXT")
  private String environment;

  /**
   * The project this bug belongs to. Direct project association allows bugs to exist
   * without requiring a cycle, pitch, or task - useful for Kanban projects and
   * general/smoke testing.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  /** The pitch this bug is associated with (optional, Shape Up specific). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /** The cycle this bug was found in. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private Cycle cycle;

  /** The team responsible for fixing this bug. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  /** The test run that discovered this bug (if any). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_run_id")
  private TestRun testRun;

  /**
   * The scope (hill chart point) this bug is related to (optional). Links the bug to a specific
   * scope for better traceability.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scope_id")
  private HillChartPoint scope;

  /**
   * The task this bug is related to (optional). Links the bug to a specific task for better
   * traceability.
   */
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
  @Column(length = 500)
  private String tags;

  /** URL or path to screenshots/attachments. */
  @Column(columnDefinition = "TEXT")
  private String attachments;

  /** User who reported this bug. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  /** User assigned to fix this bug. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  /** Resolution notes. */
  @Column(columnDefinition = "TEXT")
  private String resolution;

  /** Date when the bug was resolved. */
  private LocalDateTime resolvedAt;

  @Column(nullable = false)
  private LocalDateTime createdAt;

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
