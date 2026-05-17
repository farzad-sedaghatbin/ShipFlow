package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Soft delete-aware methods
    @Query("SELECT t FROM Task t WHERE t.deletedAt IS NULL")
    List<Task> findAllNotDeleted();

    @Query("SELECT t FROM Task t WHERE t.deletedAt IS NULL")
    Page<Task> findAllNotDeleted(Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<Task> findByIdNotDeleted(@Param("id") Long id);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.deletedAt IS NULL")
    Page<Task> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.deletedAt IS NULL")
    List<Task> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.status = :status AND t.deletedAt IS NULL")
    Page<Task> findByCycleIdAndStatusNotDeleted(@Param("cycleId") Long cycleId, @Param("status") TaskStatus status,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.status = :status AND t.deletedAt IS NULL")
    List<Task> findByCycleIdAndStatusNotDeleted(@Param("cycleId") Long cycleId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.deletedAt IS NULL")
    List<Task> findByAssigneeIdNotDeleted(@Param("assigneeId") Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.cycle.id = :cycleId AND t.deletedAt IS NULL")
    List<Task> findByAssigneeIdAndCycleIdNotDeleted(@Param("assigneeId") Long assigneeId,
            @Param("cycleId") Long cycleId);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND (t.assignee.id = :personId OR t.pairAssignee.id = :personId) AND t.deletedAt IS NULL")
    Page<Task> findByCycleIdAndPersonIdNotDeleted(@Param("cycleId") Long cycleId, @Param("personId") Long personId,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND (t.assignee.id = :personId OR t.pairAssignee.id = :personId) AND t.deletedAt IS NULL")
    List<Task> findByCycleIdAndPersonIdNotDeleted(@Param("cycleId") Long cycleId, @Param("personId") Long personId);

    // Pageable queries
    Page<Task> findAll(Pageable pageable);

    Page<Task> findByCycleId(Long cycleId, Pageable pageable);

    Page<Task> findByCycleIdAndStatus(Long cycleId, TaskStatus status, Pageable pageable);

    Page<Task> findByCycleIdAndCategory(Long cycleId, TaskCategory category, Pageable pageable);

    Page<Task> findByCycleIdAndCategoryAndStatus(Long cycleId, TaskCategory category, TaskStatus status,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND (t.assignee.id = :personId OR t.pairAssignee.id = :personId)")
    Page<Task> findByCycleIdAndPersonId(@Param("cycleId") Long cycleId, @Param("personId") Long personId,
            Pageable pageable);

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

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Task> searchTasks(@Param("query") String query, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :personId OR t.pairAssignee.id = :personId")
    Page<Task> findByPersonId(@Param("personId") Long personId, Pageable pageable);

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

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.deletedAt IS NULL")
    List<Task> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

    // Tasks with no sprint assigned (product backlog) — restricted to SCRUM projects only
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.cycle IS NULL AND t.deletedAt IS NULL AND t.parentTask IS NULL AND t.project.projectType = 'SCRUM'")
    List<Task> findProductBacklogTasks(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Task t WHERE t.cycle.project.id = :projectId")
    Page<Task> findByProjectIdPaged(@Param("projectId") Long projectId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.project.id = :projectId AND t.category = :category")
    Page<Task> findByProjectIdAndCategory(@Param("projectId") Long projectId, @Param("category") TaskCategory category,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.deletedAt IS NULL ORDER BY "
            + "CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 END, "
            + "t.createdAt DESC")
    List<Task> findByCycleIdOrderByPriority(@Param("cycleId") Long cycleId);

    // Multi-filter queries with pagination
    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId " + "AND t.deletedAt IS NULL "
            + "AND (:statuses IS NULL OR t.status IN :statuses) "
            + "AND (:priorities IS NULL OR t.priority IN :priorities) "
            + "AND (:assigneeIds IS NULL OR t.assignee.id IN :assigneeIds) "
            + "AND (:category IS NULL OR t.category = :category)")
    Page<Task> findByCycleIdWithFilters(@Param("cycleId") Long cycleId, @Param("statuses") List<TaskStatus> statuses,
            @Param("priorities") List<TaskPriority> priorities, @Param("assigneeIds") List<Long> assigneeIds,
            @Param("category") TaskCategory category, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId " + "AND t.deletedAt IS NULL "
            + "AND (:statuses IS NULL OR t.status NOT IN :statuses) "
            + "AND (:priorities IS NULL OR t.priority NOT IN :priorities) "
            + "AND (:assigneeIds IS NULL OR t.assignee IS NULL OR t.assignee.id NOT IN :assigneeIds)")
    Page<Task> findByCycleIdWithExclusionFilters(@Param("cycleId") Long cycleId,
            @Param("statuses") List<TaskStatus> statuses, @Param("priorities") List<TaskPriority> priorities,
            @Param("assigneeIds") List<Long> assigneeIds, Pageable pageable);

    // Project-based multi-filter queries with pagination
    @Query("SELECT t FROM Task t WHERE t.cycle.project.id = :projectId " 
            + "AND t.deletedAt IS NULL "
            + "AND (:statuses IS NULL OR t.status IN :statuses) "
            + "AND (:priorities IS NULL OR t.priority IN :priorities) "
            + "AND (:assigneeIds IS NULL OR t.assignee.id IN :assigneeIds) "
            + "AND (:category IS NULL OR t.category = :category)")
    Page<Task> findByProjectIdWithFilters(@Param("projectId") Long projectId, 
            @Param("statuses") List<TaskStatus> statuses,
            @Param("priorities") List<TaskPriority> priorities, 
            @Param("assigneeIds") List<Long> assigneeIds,
            @Param("category") TaskCategory category, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.project.id = :projectId " 
            + "AND t.deletedAt IS NULL "
            + "AND (:statuses IS NULL OR t.status NOT IN :statuses) "
            + "AND (:priorities IS NULL OR t.priority NOT IN :priorities) "
            + "AND (:assigneeIds IS NULL OR t.assignee IS NULL OR t.assignee.id NOT IN :assigneeIds)")
    Page<Task> findByProjectIdWithExclusionFilters(@Param("projectId") Long projectId,
            @Param("statuses") List<TaskStatus> statuses, 
            @Param("priorities") List<TaskPriority> priorities,
            @Param("assigneeIds") List<Long> assigneeIds, Pageable pageable);

    // Hierarchy queries
    List<Task> findByParentTaskId(Long parentTaskId);

    @Query("SELECT t FROM Task t WHERE t.parentTask.id = :parentTaskId AND t.deletedAt IS NULL")
    List<Task> findByParentTaskIdNotDeleted(@Param("parentTaskId") Long parentTaskId);

    List<Task> findByCycleIdAndParentTaskIdIsNull(Long cycleId);

    Page<Task> findByCycleIdAndParentTaskIdIsNull(Long cycleId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.cycle.id = :cycleId AND t.parentTask IS NULL")
    List<Task> findRootTasksByCycleId(@Param("cycleId") Long cycleId);

    // Pitch and scope queries for traceability
    List<Task> findByPitchId(Long pitchId);

    List<Task> findByScopeId(Long scopeId);

    @Query("SELECT t FROM Task t WHERE t.pitch.id = :pitchId AND t.status = :status")
    List<Task> findByPitchIdAndStatus(@Param("pitchId") Long pitchId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.scope.id = :scopeId AND t.status = :status")
    List<Task> findByScopeIdAndStatus(@Param("scopeId") Long scopeId, @Param("status") TaskStatus status);

    // Release-related queries
    @Query("SELECT t FROM Task t WHERE t.targetRelease.id = :releaseId AND t.deletedAt IS NULL")
    List<Task> findByTargetReleaseIdNotDeleted(@Param("releaseId") Long releaseId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.targetRelease.id = :releaseId AND t.deletedAt IS NULL")
    long countByTargetReleaseIdNotDeleted(@Param("releaseId") Long releaseId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.targetRelease.id = :releaseId AND t.status = :status AND t.deletedAt IS NULL")
    long countByTargetReleaseIdAndStatusNotDeleted(@Param("releaseId") Long releaseId, @Param("status") TaskStatus status);

    @Query("SELECT t.targetRelease.id, COUNT(t) FROM Task t WHERE t.targetRelease.id IN :releaseIds AND t.deletedAt IS NULL GROUP BY t.targetRelease.id")
    List<Object[]> countByTargetReleaseIdsNotDeleted(@Param("releaseIds") List<Long> releaseIds);
}
