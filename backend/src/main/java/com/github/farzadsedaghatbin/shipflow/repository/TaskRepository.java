package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Pageable queries
    Page<Task> findAll(Pageable pageable);
    
    Page<Task> findByCycleId(Long cycleId, Pageable pageable);
    
    Page<Task> findByCycleIdAndStatus(Long cycleId, TaskStatus status, Pageable pageable);
    
    Page<Task> findByCycleIdAndCategory(Long cycleId, TaskCategory category, Pageable pageable);
    
    Page<Task> findByCycleIdAndCategoryAndStatus(Long cycleId, TaskCategory category, TaskStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND (t.assignee.id = :personId OR t.pairAssignee.id = :personId)")
    Page<Task> findByCycleIdAndPersonId(@Param("cycleId") Long cycleId, @Param("personId") Long personId, Pageable pageable);
    
    // Non-pageable queries (for backward compatibility)
    List<Task> findByCycleId(Long cycleId);
    
    List<Task> findByCycleIdAndStatus(Long cycleId, TaskStatus status);
    
    List<Task> findByCycleIdAndCategory(Long cycleId, TaskCategory category);
    
    List<Task> findByAssigneeId(Long assigneeId);
    
    List<Task> findByAssigneeIdAndCycleId(Long assigneeId, Long cycleId);
    
    List<Task> findByPairAssigneeId(Long pairAssigneeId);
    
    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND (t.assignee.id = :personId OR t.pairAssignee.id = :personId)")
    List<Task> findByCycleIdAndPersonId(@Param("cycleId") Long cycleId, @Param("personId") Long personId);
    
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :personId OR t.pairAssignee.id = :personId")
    List<Task> findByPersonId(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.cycle.id = :cycleId")
    int countByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.cycle.id = :cycleId AND t.status = :status")
    int countByCycleIdAndStatus(@Param("cycleId") Long cycleId, @Param("status") TaskStatus status);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.cycle.id = :cycleId AND t.category = :category")
    int countByCycleIdAndCategory(@Param("cycleId") Long cycleId, @Param("category") TaskCategory category);
    
    @Query("SELECT COALESCE(SUM(t.estimateHours), 0) FROM Task t WHERE t.cycle.id = :cycleId")
    Double getTotalEstimateHoursByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT COALESCE(SUM(t.actualHours), 0) FROM Task t WHERE t.cycle.id = :cycleId")
    Double getTotalActualHoursByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT COUNT(DISTINCT t.assignee.id) FROM Task t WHERE t.cycle.id = :cycleId AND t.assignee IS NOT NULL")
    int countDistinctAssigneesByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT t FROM Task t WHERE t.cycle.project.id = :projectId")
    List<Task> findByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId ORDER BY " +
           "CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END, " +
           "t.createdAt DESC")
    List<Task> findByCycleIdOrderByPriority(@Param("cycleId") Long cycleId);

    // Multi-filter queries with pagination
    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId " +
           "AND (:statuses IS NULL OR t.status IN :statuses) " +
           "AND (:priorities IS NULL OR t.priority IN :priorities) " +
           "AND (:assigneeIds IS NULL OR t.assignee.id IN :assigneeIds) " +
           "AND (:category IS NULL OR t.category = :category)")
    Page<Task> findByCycleIdWithFilters(
        @Param("cycleId") Long cycleId,
        @Param("statuses") List<TaskStatus> statuses,
        @Param("priorities") List<TaskPriority> priorities,
        @Param("assigneeIds") List<Long> assigneeIds,
        @Param("category") TaskCategory category,
        Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId " +
           "AND (:statuses IS NULL OR t.status NOT IN :statuses) " +
           "AND (:priorities IS NULL OR t.priority NOT IN :priorities) " +
           "AND (:assigneeIds IS NULL OR t.assignee.id NOT IN :assigneeIds)")
    Page<Task> findByCycleIdWithExclusionFilters(
        @Param("cycleId") Long cycleId,
        @Param("statuses") List<TaskStatus> statuses,
        @Param("priorities") List<TaskPriority> priorities,
        @Param("assigneeIds") List<Long> assigneeIds,
        Pageable pageable);
}
