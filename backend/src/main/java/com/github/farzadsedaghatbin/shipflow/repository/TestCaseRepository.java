package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for TestCase entity. */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

  // Soft delete-aware methods
  @Query("SELECT tc FROM TestCase tc WHERE tc.deletedAt IS NULL")
  List<TestCase> findAllNotDeleted();

  @Query("SELECT tc FROM TestCase tc WHERE tc.id = :id AND tc.deletedAt IS NULL")
  Optional<TestCase> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT tc FROM TestCase tc WHERE tc.testCaseKey = :testCaseKey AND tc.deletedAt IS NULL")
  Optional<TestCase> findByTestCaseKeyNotDeleted(@Param("testCaseKey") String testCaseKey);

  @Query("SELECT tc FROM TestCase tc WHERE tc.pitch.id = :pitchId AND tc.deletedAt IS NULL")
  List<TestCase> findByPitchIdNotDeleted(@Param("pitchId") Long pitchId);

  @Query("SELECT tc FROM TestCase tc WHERE tc.cycle.id = :cycleId AND tc.deletedAt IS NULL")
  List<TestCase> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId);

  @Query("SELECT tc FROM TestCase tc WHERE tc.team.id = :teamId AND tc.deletedAt IS NULL")
  List<TestCase> findByTeamIdNotDeleted(@Param("teamId") Long teamId);

  @Query("SELECT tc FROM TestCase tc WHERE tc.status = :status AND tc.deletedAt IS NULL")
  List<TestCase> findByStatusNotDeleted(@Param("status") TestCaseStatus status);

  @Query("SELECT tc FROM TestCase tc WHERE tc.type = :type AND tc.deletedAt IS NULL")
  List<TestCase> findByTypeNotDeleted(@Param("type") TestCaseType type);

  @Query("SELECT tc FROM TestCase tc WHERE tc.createdBy.id = :userId AND tc.deletedAt IS NULL")
  List<TestCase> findByCreatedByIdNotDeleted(@Param("userId") Long userId);

  // Standard methods (include deleted entities for backward compatibility)
  Optional<TestCase> findByTestCaseKey(String testCaseKey);

  List<TestCase> findByPitchId(Long pitchId);

  List<TestCase> findByCycleId(Long cycleId);

  List<TestCase> findByTeamId(Long teamId);

  List<TestCase> findByStatus(TestCaseStatus status);

  List<TestCase> findByType(TestCaseType type);

  List<TestCase> findByCreatedById(Long userId);

  List<TestCase> findByPitchIdAndStatus(Long pitchId, TestCaseStatus status);

  List<TestCase> findByCycleIdAndStatus(Long cycleId, TestCaseStatus status);

  @Query("SELECT tc FROM TestCase tc WHERE tc.aiGenerated = true")
  List<TestCase> findAllAiGenerated();

  @Query("SELECT tc FROM TestCase tc WHERE tc.pitch.id = :pitchId AND tc.aiGenerated = true")
  List<TestCase> findAiGeneratedByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.pitch.id = :pitchId")
  long countByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.cycle.id = :cycleId")
  long countByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.pitch.id = :pitchId AND tc.status = :status")
  long countByPitchIdAndStatus(@Param("pitchId") Long pitchId, @Param("status") TestCaseStatus status);

  @Query("SELECT tc FROM TestCase tc WHERE tc.tags LIKE %:tag%")
  List<TestCase> findByTag(@Param("tag") String tag);

  @Query("SELECT DISTINCT tc.tags FROM TestCase tc WHERE tc.tags IS NOT NULL")
  List<String> findAllTags();

  @Query("SELECT MAX(CAST(SUBSTRING(tc.testCaseKey, 4) AS int)) FROM TestCase tc WHERE tc.testCaseKey LIKE 'TC-%'")
  Integer findMaxTestCaseKeyNumber();

  boolean existsByTestCaseKey(String testCaseKey);

  @Query("SELECT tc FROM TestCase tc WHERE tc.importJob.id = :importJobId AND tc.deletedAt IS NULL")
  List<TestCase> findByImportJobId(@Param("importJobId") Long importJobId);

  // Multi-filter query
  @Query("SELECT tc FROM TestCase tc WHERE tc.deletedAt IS NULL " + "AND (:cycleId IS NULL OR tc.cycle.id = :cycleId) "
      + "AND (:pitchId IS NULL OR tc.pitch.id = :pitchId) " + "AND (:statuses IS NULL OR tc.status IN :statuses) "
      + "AND (:types IS NULL OR tc.type IN :types) " + "AND (:priorities IS NULL OR tc.priority IN :priorities)")
  List<TestCase> findWithFilters(@Param("cycleId") Long cycleId, @Param("pitchId") Long pitchId,
      @Param("statuses") List<TestCaseStatus> statuses, @Param("types") List<TestCaseType> types,
      @Param("priorities") List<TestCasePriority> priorities);

  // Scope and task queries for traceability
  List<TestCase> findByScopeId(Long scopeId);

  List<TestCase> findByTaskId(Long taskId);

  @Query("SELECT tc FROM TestCase tc WHERE tc.scope.id = :scopeId AND tc.status = :status")
  List<TestCase> findByScopeIdAndStatus(@Param("scopeId") Long scopeId, @Param("status") TestCaseStatus status);

  @Query("SELECT tc FROM TestCase tc WHERE tc.task.id = :taskId AND tc.status = :status")
  List<TestCase> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") TestCaseStatus status);

  @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.scope.id = :scopeId")
  long countByScopeId(@Param("scopeId") Long scopeId);

  @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.task.id = :taskId")
  long countByTaskId(@Param("taskId") Long taskId);
}
