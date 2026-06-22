package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for BugReport entity. */
@Repository
public interface BugReportRepository extends JpaRepository<BugReport, Long>, JpaSpecificationExecutor<BugReport> {

  // Pageable queries
  Page<BugReport> findAll(Pageable pageable);

  Page<BugReport> findByPitchId(Long pitchId, Pageable pageable);

  Page<BugReport> findByCycleId(Long cycleId, Pageable pageable);

  Optional<BugReport> findByBugKey(String bugKey);

  List<BugReport> findByPitchId(Long pitchId);

  List<BugReport> findByCycleId(Long cycleId);

  List<BugReport> findByTeamId(Long teamId);

  List<BugReport> findByStatus(BugStatus status);

  List<BugReport> findBySeverity(BugSeverity severity);

  List<BugReport> findByReporterId(Long reporterId);

  List<BugReport> findByAssigneeId(Long assigneeId);

  List<BugReport> findByTestRunId(Long testRunId);

  List<BugReport> findByPitchIdAndStatus(Long pitchId, BugStatus status);

  List<BugReport> findByCycleIdAndStatus(Long cycleId, BugStatus status);

  @Query("SELECT br FROM BugReport br WHERE br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  List<BugReport> findAllOpen();

  @Query("SELECT br FROM BugReport br WHERE br.pitch.id = :pitchId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  List<BugReport> findOpenByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT br FROM BugReport br WHERE br.cycle.id = :cycleId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  List<BugReport> findOpenByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.pitch.id = :pitchId")
  long countByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.cycle.id = :cycleId")
  long countByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.pitch.id = :pitchId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  long countOpenByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.cycle.id = :cycleId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  long countOpenByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.pitch.id = :pitchId AND br.severity = :severity")
  long countByPitchIdAndSeverity(@Param("pitchId") Long pitchId, @Param("severity") BugSeverity severity);

  @Query("SELECT br FROM BugReport br WHERE br.tags LIKE %:tag%")
  List<BugReport> findByTag(@Param("tag") String tag);

  @Query("SELECT MAX(CAST(SUBSTRING(br.bugKey, 5) AS int)) FROM BugReport br WHERE br.bugKey LIKE 'BUG-%'")
  Integer findMaxBugKeyNumber();

  // Task queries for traceability
  List<BugReport> findByTaskId(Long taskId);

  @Query("SELECT br FROM BugReport br WHERE br.task.id = :taskId AND br.status = :status")
  List<BugReport> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") BugStatus status);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.task.id = :taskId")
  long countByTaskId(@Param("taskId") Long taskId);

  boolean existsByBugKey(String bugKey);

  // Multi-filter queries with pagination - supports direct project association
  // Use LEFT JOIN FETCH to avoid implicit inner joins on nullable relationships
  @Query("SELECT br FROM BugReport br " + "LEFT JOIN br.cycle c " + "LEFT JOIN br.pitch p "
      + "LEFT JOIN br.assignee a " + "WHERE 1=1 "
      + "AND (:projectId IS NULL OR br.project.id = :projectId OR (c IS NOT NULL AND c.project.id = :projectId)) "
      + "AND (:cycleId IS NULL OR c.id = :cycleId) " + "AND (:pitchId IS NULL OR p.id = :pitchId) "
      + "AND (:statuses IS NULL OR br.status IN :statuses) "
      + "AND (:severities IS NULL OR br.severity IN :severities) "
      + "AND (:assigneeIds IS NULL OR a.id IN :assigneeIds) "
      + "AND (:search IS NULL OR LOWER(br.title) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR LOWER(br.description) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR LOWER(br.bugKey) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
  Page<BugReport> findWithFilters(@Param("projectId") Long projectId, @Param("cycleId") Long cycleId,
      @Param("pitchId") Long pitchId, @Param("statuses") List<BugStatus> statuses,
      @Param("severities") List<BugSeverity> severities, @Param("assigneeIds") List<Long> assigneeIds,
      @Param("search") String search, Pageable pageable);

  @Query("SELECT br FROM BugReport br " + "LEFT JOIN br.cycle c " + "LEFT JOIN br.pitch p "
      + "LEFT JOIN br.assignee a " + "WHERE 1=1 "
      + "AND (:projectId IS NULL OR br.project.id = :projectId OR (c IS NOT NULL AND c.project.id = :projectId)) "
      + "AND (:cycleId IS NULL OR c.id = :cycleId) " + "AND (:pitchId IS NULL OR p.id = :pitchId) "
      + "AND (:statuses IS NULL OR br.status NOT IN :statuses) "
      + "AND (:severities IS NULL OR br.severity NOT IN :severities) "
      + "AND (:assigneeIds IS NULL OR a.id NOT IN :assigneeIds) "
      + "AND (:search IS NULL OR LOWER(br.title) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR LOWER(br.description) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR LOWER(br.bugKey) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
  Page<BugReport> findWithExclusionFilters(@Param("projectId") Long projectId, @Param("cycleId") Long cycleId,
      @Param("pitchId") Long pitchId, @Param("statuses") List<BugStatus> statuses,
      @Param("severities") List<BugSeverity> severities, @Param("assigneeIds") List<Long> assigneeIds,
      @Param("search") String search, Pageable pageable);

  // Direct project queries
  List<BugReport> findByProjectId(Long projectId);

  Page<BugReport> findByProjectId(Long projectId, Pageable pageable);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.project.id = :projectId")
  long countByProjectId(@Param("projectId") Long projectId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.project.id = :projectId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  long countOpenByProjectId(@Param("projectId") Long projectId);

  // Release-related queries
  List<BugReport> findByTargetReleaseId(Long releaseId);

  List<BugReport> findByFixedInReleaseId(Long releaseId);

  @Query("SELECT br FROM BugReport br WHERE br.targetRelease.id = :releaseId AND br.status = :status")
  List<BugReport> findByTargetReleaseIdAndStatus(@Param("releaseId") Long releaseId, @Param("status") BugStatus status);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.targetRelease.id = :releaseId")
  long countByTargetReleaseId(@Param("releaseId") Long releaseId);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.targetRelease.id = :releaseId AND br.status = :status")
  long countByTargetReleaseIdAndStatus(@Param("releaseId") Long releaseId, @Param("status") BugStatus status);

  @Query("SELECT COUNT(br) FROM BugReport br WHERE br.targetRelease.id = :releaseId AND br.status NOT IN ('CLOSED', 'RESOLVED', 'VERIFIED', 'WONT_FIX', 'DUPLICATE')")
  long countOpenByTargetReleaseId(@Param("releaseId") Long releaseId);

  /** Find bugs whose fixed release differs from target (slipped bugs) */
  @Query("SELECT br FROM BugReport br WHERE br.targetRelease.id = :releaseId AND br.fixedInRelease IS NOT NULL AND br.fixedInRelease.id <> br.targetRelease.id")
  List<BugReport> findSlippedBugsByTargetReleaseId(@Param("releaseId") Long releaseId);

  @Query("SELECT br.targetRelease.id, COUNT(br) FROM BugReport br WHERE br.targetRelease.id IN :releaseIds GROUP BY br.targetRelease.id")
  List<Object[]> countByTargetReleaseIds(@Param("releaseIds") List<Long> releaseIds);
}
