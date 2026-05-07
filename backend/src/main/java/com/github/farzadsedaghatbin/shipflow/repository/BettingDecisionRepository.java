package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.BettingDecision;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BettingDecisionRepository extends JpaRepository<BettingDecision, Long> {

  /** Find all decisions for a specific pitch (across all cycles) */
  List<BettingDecision> findByPitchIdOrderByDecidedAtDesc(Long pitchId);

  /** Find all decisions for a specific cycle */
  List<BettingDecision> findByCycleIdOrderByDecidedAtDesc(Long cycleId);

  /** Find all decisions for a pitch in a specific cycle */
  List<BettingDecision> findByPitchIdAndCycleIdOrderByDecidedAtDesc(Long pitchId, Long cycleId);

  /** Find the single decision for a pitch in a specific cycle (optimized for unique constraint) */
  Optional<BettingDecision> findByPitchIdAndCycleId(Long pitchId, Long cycleId);

  /** Find the most recent decision for a pitch */
  Optional<BettingDecision> findFirstByPitchIdOrderByDecidedAtDesc(Long pitchId);

  /** Find all committed pitches for a cycle */
  @Query("SELECT bd FROM BettingDecision bd WHERE bd.cycle.id = :cycleId AND bd.decision = 'COMMITTED' ORDER BY bd.decidedAt DESC")
  List<BettingDecision> findCommittedByCycleId(@Param("cycleId") Long cycleId);

  /** Find all rejected pitches for a cycle */
  @Query("SELECT bd FROM BettingDecision bd WHERE bd.cycle.id = :cycleId AND bd.decision = 'REJECTED' ORDER BY bd.decidedAt DESC")
  List<BettingDecision> findRejectedByCycleId(@Param("cycleId") Long cycleId);

  /** Find decisions by type for a cycle */
  List<BettingDecision> findByCycleIdAndDecisionOrderByDecidedAtDesc(Long cycleId, BettingDecisionType decision);

  /** Find decisions by user who made them */
  List<BettingDecision> findByDecidedByIdOrderByDecidedAtDesc(Long userId);

  /** Find decisions made within a date range */
  @Query("SELECT bd FROM BettingDecision bd WHERE bd.decidedAt BETWEEN :startDate AND :endDate ORDER BY bd.decidedAt DESC")
  List<BettingDecision> findByDecidedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /** Count decisions by type for a cycle */
  @Query("SELECT bd.decision, COUNT(bd) FROM BettingDecision bd WHERE bd.cycle.id = :cycleId GROUP BY bd.decision")
  List<Object[]> countDecisionsByCycleIdGroupByType(@Param("cycleId") Long cycleId);

  /** Find decisions for a team (considered team) */
  List<BettingDecision> findByConsideredTeamIdOrderByDecidedAtDesc(Long teamId);

  /** Find pitches that were repeatedly rejected (rejected more than N times) */
  @Query("SELECT bd.pitch.id, COUNT(bd) as rejectCount FROM BettingDecision bd " +
         "WHERE bd.decision = 'REJECTED' GROUP BY bd.pitch.id HAVING COUNT(bd) >= :minRejects")
  List<Object[]> findRepeatedlyRejectedPitches(@Param("minRejects") long minRejects);

  /** Find decisions where appetite exceeded capacity */
  @Query("SELECT bd FROM BettingDecision bd WHERE bd.requestedAppetiteDays > bd.availableCapacityDays ORDER BY bd.decidedAt DESC")
  List<BettingDecision> findOverCapacityDecisions();

  /** Paginated history for a project (via cycle -> project relationship) */
  @Query("SELECT bd FROM BettingDecision bd WHERE bd.cycle.project.id = :projectId ORDER BY bd.decidedAt DESC")
  Page<BettingDecision> findByProjectId(@Param("projectId") Long projectId, Pageable pageable);

  /** Check if a decision already exists for a pitch in a cycle */
  boolean existsByPitchIdAndCycleId(Long pitchId, Long cycleId);

  /** Count all decisions for a cycle (for phase transition validation) */
  long countByCycleId(Long cycleId);

  /** Count committed decisions for a cycle */
  @Query("SELECT COUNT(bd) FROM BettingDecision bd WHERE bd.cycle.id = :cycleId AND bd.decision = 'COMMITTED'")
  long countCommittedByCycleId(@Param("cycleId") Long cycleId);
}
