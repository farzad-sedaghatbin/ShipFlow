package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.TestRun;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for TestRun entity. */
@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

  List<TestRun> findByTestCaseId(Long testCaseId);

  List<TestRun> findByPitchId(Long pitchId);

  List<TestRun> findByCycleId(Long cycleId);

  List<TestRun> findByStatus(TestRunStatus status);

  List<TestRun> findByExecutedById(Long userId);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.testCase.id = :testCaseId ORDER BY tr.executedAt DESC")
  List<TestRun> findByTestCaseIdOrderByExecutedAtDesc(@Param("testCaseId") Long testCaseId);

  @Query("SELECT tr FROM TestRun tr WHERE tr.testCase.id = :testCaseId ORDER BY tr.executedAt DESC LIMIT 1")
  TestRun findLatestByTestCaseId(@Param("testCaseId") Long testCaseId);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.pitch.id = :pitchId ORDER BY tr.executedAt DESC")
  List<TestRun> findByPitchIdOrderByExecutedAtDesc(@Param("pitchId") Long pitchId);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.cycle.id = :cycleId ORDER BY tr.executedAt DESC")
  List<TestRun> findByCycleIdOrderByExecutedAtDesc(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.testCase.id = :testCaseId")
  long countByTestCaseId(@Param("testCaseId") Long testCaseId);

  @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.pitch.id = :pitchId")
  long countByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.cycle.id = :cycleId")
  long countByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.pitch.id = :pitchId AND tr.status = :status")
  long countByPitchIdAndStatus(@Param("pitchId") Long pitchId, @Param("status") TestRunStatus status);

  @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.cycle.id = :cycleId AND tr.status = :status")
  long countByCycleIdAndStatus(@Param("cycleId") Long cycleId, @Param("status") TestRunStatus status);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.executedAt >= :fromDate AND tr.executedAt <= :toDate ORDER BY tr.executedAt DESC")
  List<TestRun> findByExecutedAtBetween(@Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.pitch.id = :pitchId AND tr.status = 'PASSED' ORDER BY tr.executedAt DESC")
  List<TestRun> findPassedByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT tr FROM TestRun tr LEFT JOIN FETCH tr.testCase LEFT JOIN FETCH tr.pitch LEFT JOIN FETCH tr.cycle LEFT JOIN FETCH tr.executedBy WHERE tr.pitch.id = :pitchId AND tr.status = 'FAILED' ORDER BY tr.executedAt DESC")
  List<TestRun> findFailedByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT AVG(tr.durationSeconds) FROM TestRun tr WHERE tr.testCase.id = :testCaseId AND tr.durationSeconds IS NOT NULL")
  Double findAvgDurationByTestCaseId(@Param("testCaseId") Long testCaseId);
}
