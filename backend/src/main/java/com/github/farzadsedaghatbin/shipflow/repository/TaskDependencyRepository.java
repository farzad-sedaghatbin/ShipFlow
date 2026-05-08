package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

  /**
   * Find all dependencies where the given task is the source (it blocks or
   * depends on others).
   */
  List<TaskDependency> findBySourceTaskId(Long sourceTaskId);

  /**
   * Find all dependencies where the given task is the target (it is blocked by or
   * depended upon by others).
   */
  List<TaskDependency> findByTargetTaskId(Long targetTaskId);

  /** Find a specific dependency between two tasks. */
  Optional<TaskDependency> findBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId);

  /**
   * Find all tasks that are blocking the given task (dependencies where this task
   * is the source and type is BLOCKS).
   */
  @Query("SELECT td FROM TaskDependency td WHERE td.sourceTask.id = :taskId AND td.dependencyType = 'BLOCKS'")
  List<TaskDependency> findBlockingDependenciesByTaskId(@Param("taskId") Long taskId);

  /**
   * Find all tasks that are blocked by the given task (dependencies where this
   * task is the target and type is BLOCKS).
   */
  @Query("SELECT td FROM TaskDependency td WHERE td.targetTask.id = :taskId AND td.dependencyType = 'BLOCKS'")
  List<TaskDependency> findBlockedByDependenciesByTaskId(@Param("taskId") Long taskId);

  /**
   * Find all dependencies where the given task is either the source or the target.
   * Used for dependency constraint validation during reorder operations.
   */
  List<TaskDependency> findBySourceTaskIdOrTargetTaskId(Long sourceTaskId, Long targetTaskId);

  /**
   * Batch fetch all dependencies where any of the given task IDs appears as source
   * or target. Used to avoid N+1 queries during reorder validation.
   */
  List<TaskDependency> findBySourceTaskIdInOrTargetTaskIdIn(
      java.util.Collection<Long> sourceIds, java.util.Collection<Long> targetIds);

  /** Delete a specific dependency between two tasks. */
  void deleteBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId);

  /** Check if a dependency exists between two tasks (in either direction). */
  @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END FROM TaskDependency td "
      + "WHERE (td.sourceTask.id = :task1Id AND td.targetTask.id = :task2Id) "
      + "OR (td.sourceTask.id = :task2Id AND td.targetTask.id = :task1Id)")
  boolean existsBetweenTasks(@Param("task1Id") Long task1Id, @Param("task2Id") Long task2Id);
}
