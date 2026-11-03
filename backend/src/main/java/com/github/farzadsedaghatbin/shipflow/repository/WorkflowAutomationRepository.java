package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowAutomationRepository extends JpaRepository<WorkflowAutomation, Long> {

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.project.id = :projectId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
  List<WorkflowAutomation> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.project.id = :projectId AND a.enabled = true AND a.deletedAt IS NULL AND a.triggerType = :triggerType")
  List<WorkflowAutomation> findEnabledByProjectIdAndTriggerType(
      @Param("projectId") Long projectId, @Param("triggerType") TriggerType triggerType);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.enabled = true AND a.deletedAt IS NULL AND a.triggerType = :triggerType")
  List<WorkflowAutomation> findEnabledByTriggerType(@Param("triggerType") TriggerType triggerType);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.id = :id AND a.deletedAt IS NULL")
  Optional<WorkflowAutomation> findByIdNotDeleted(@Param("id") Long id);

  /** Enabled, non-deleted automation count — the number that counts against the licence automation cap. */
  @Query("SELECT COUNT(a) FROM WorkflowAutomation a WHERE a.enabled = true AND a.deletedAt IS NULL")
  long countEnabledNotDeleted();

  // ── Scheduling (S71) ─────────────────────────────────────────────────────

  /** SCHEDULE automations whose slot has arrived. */
  @Query("SELECT a FROM WorkflowAutomation a WHERE a.enabled = true AND a.deletedAt IS NULL"
      + " AND a.triggerType = :triggerType AND a.nextRunAt IS NOT NULL AND a.nextRunAt <= :now")
  List<WorkflowAutomation> findDueScheduled(
      @Param("triggerType") TriggerType triggerType, @Param("now") Instant now);

  /** Enabled SCHEDULE automations that have no next slot yet (e.g. enabled before this existed). */
  @Query("SELECT a FROM WorkflowAutomation a WHERE a.enabled = true AND a.deletedAt IS NULL"
      + " AND a.triggerType = :triggerType AND a.nextRunAt IS NULL")
  List<WorkflowAutomation> findScheduledWithoutNextRun(@Param("triggerType") TriggerType triggerType);

  /**
   * Moves a due automation to its next slot, but only if nobody else already did. Returns 1 for
   * the single caller that won — which is what makes it safe to run the scheduler on every replica.
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE WorkflowAutomation a SET a.nextRunAt = :next WHERE a.id = :id AND a.nextRunAt = :expected")
  int claimSlot(@Param("id") Long id, @Param("expected") Instant expected, @Param("next") Instant next);

  /** Gives a never-scheduled automation its first slot (no-op if it already has one). */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE WorkflowAutomation a SET a.nextRunAt = :next WHERE a.id = :id AND a.nextRunAt IS NULL")
  int initSlot(@Param("id") Long id, @Param("next") Instant next);

  /**
   * Counts one run without rewriting the rest of the row. Saving the whole (detached) entity
   * instead would write back a stale {@code nextRunAt} and silently undo the scheduler's claim.
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE WorkflowAutomation a SET a.executionCount = a.executionCount + 1, a.lastTriggeredAt = :at WHERE a.id = :id")
  int recordExecution(@Param("id") Long id, @Param("at") java.time.LocalDateTime at);
}
