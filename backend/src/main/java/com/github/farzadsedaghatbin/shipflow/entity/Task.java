package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskCategory category;

  @NotAudited
  @Column(precision = 5, scale = 2)
  private BigDecimal estimateHours;

  @NotAudited
  @Column(precision = 5, scale = 2)
  private BigDecimal actualHours;

  /**
   * The cycle this task belongs to. For Kanban projects, tasks are associated with a long-term
   * hidden cycle for data consistency, but this cycle is never shown in the UI.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private Cycle cycle;

  /**
   * The pitch this task is associated with (optional). Links the task to a
   * specific pitch for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /**
   * The scope (hill chart point) this task is associated with (optional). Links
   * the task to a specific scope for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scope_id")
  private HillChartPoint scope;

  /**
   * The scope that was auto-created when this root task was linked to a pitch.
   * Used for bidirectional Scope-Task synchronization.
   */
  @NotAudited
  @OneToOne(fetch = FetchType.LAZY, mappedBy = "linkedTask")
  private HillChartPoint autoCreatedScope;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private Person assignee;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pair_assignee_id")
  private Person pairAssignee;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private Person createdBy;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_task_id")
  private Task parentTask;

  @NotAudited
  @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @org.hibernate.annotations.BatchSize(size = 25)
  private java.util.List<Task> children = new java.util.ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "sourceTask", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @org.hibernate.annotations.BatchSize(size = 25)
  private java.util.List<TaskDependency> outgoingDependencies = new java.util.ArrayList<>();

  @NotAudited
  @OneToMany(mappedBy = "targetTask", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @org.hibernate.annotations.BatchSize(size = 25)
  private java.util.List<TaskDependency> incomingDependencies = new java.util.ArrayList<>();

  @NotAudited
  @Column
  private LocalDate dueDate;

  @NotAudited
  @Column
  private LocalDateTime completedAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String tags;

  /** The target release for this task (optional) */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_release_id")
  private Release targetRelease;

  // Soft delete fields
  @NotAudited
  @Column
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

  /**
   * Checks if this task is a root scope task (shows on hill chart).
   * A task is a root scope if it has a pitch and no parent task.
   */
  public boolean isRootScope() {
    return pitch != null && parentTask == null;
  }

  /**
   * Checks if this task should be displayed on the hill chart.
   * True if it's a root scope task with an auto-created scope.
   */
  public boolean showsOnHillChart() {
    return isRootScope() && autoCreatedScope != null;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = TaskStatus.BACKLOG;
    }
    if (priority == null) {
      priority = TaskPriority.MEDIUM;
    }
    if (category == null) {
      category = TaskCategory.PITCH_SCOPE;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    if (status == TaskStatus.DONE && completedAt == null) {
      completedAt = LocalDateTime.now();
    }
  }
}
