package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Audit trail of the cycle (and status) a task belonged to at a given point in time. Unlike
 * {@code Task.cycle} (which is {@code @NotAudited} and only reflects the current value), this
 * table accumulates one row per meaningful cycle-related event so future reporting can answer
 * "which cycle was this task in, and in which cycle did it complete" — see the Task/Pitch cycle
 * model change (no task requires a cycle at creation; pitch-linked tasks derive their cycle from
 * the pitch's current bet and follow it as the pitch is re-bet).
 *
 * <p>Snapshots are created via direct, synchronous service calls (not event listeners) —
 * mirrors {@link PitchRiskHistory}.
 */
@Entity
@Table(name = "task_cycle_history", indexes = {
    @Index(name = "idx_task_cycle_history_task", columnList = "task_id"),
    @Index(name = "idx_task_cycle_history_cycle", columnList = "cycle_id"),
    @Index(name = "idx_task_cycle_history_task_date", columnList = "task_id, recorded_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCycleHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cycle", "project", "pitch", "scope", "assignee",
      "pairAssignee", "team", "createdBy", "deletedBy", "parentTask", "children", "outgoingDependencies",
      "incomingDependencies"})
  private Task task;

  /** The cycle the task belonged to at this moment, or {@code null} for "no cycle". */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = true)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "project"})
  private Cycle cycle;

  /** Snapshot of {@code task.status} at the moment this row was recorded. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  /** What triggered this cycle-history snapshot. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ChangeSource source;

  @Column(nullable = false)
  private LocalDateTime recordedAt;

  @PrePersist
  protected void onCreate() {
    if (recordedAt == null) {
      recordedAt = LocalDateTime.now();
    }
  }

  /** What triggered a cycle-history snapshot. */
  public enum ChangeSource {
    /** Task was just created. */
    TASK_CREATED,
    /** Task status changed (via full update, PATCH /status, or bulk action). */
    STATUS_CHANGE,
    /** The task's cycle changed because its pitch was assigned/re-assigned/unassigned to a cycle. */
    PITCH_CYCLE_CHANGE,
    /** A user explicitly changed the task's cycle (or its pitch, which re-derived the cycle). */
    MANUAL_CYCLE_CHANGE
  }
}
