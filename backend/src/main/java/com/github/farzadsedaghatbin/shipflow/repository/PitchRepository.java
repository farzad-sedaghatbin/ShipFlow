package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {

  // Standard find methods (will return deleted entities)
  List<Pitch> findByCycleId(Long cycleId);

  List<Pitch> findByTeamId(Long teamId);

  List<Pitch> findByCycleIdAndStatus(Long cycleId, PitchStatus status);

  // Soft delete-aware methods
  @Query("SELECT p FROM Pitch p WHERE p.deletedAt IS NULL")
  List<Pitch> findAllNotDeleted();

  @Query("SELECT p FROM Pitch p WHERE p.id = :id AND p.deletedAt IS NULL")
  Optional<Pitch> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT p FROM Pitch p LEFT JOIN FETCH p.team t LEFT JOIN FETCH t.assignments WHERE p.id = :id AND p.deletedAt IS NULL")
  Optional<Pitch> findByIdWithTeamAndAssignments(@Param("id") Long id);

  @Query("SELECT DISTINCT p FROM Pitch p WHERE p.deletedAt IS NULL AND (p.cycle.id = :cycleId OR EXISTS (SELECT s FROM BettingSlot s WHERE s.pitch = p AND s.cycle.id = :cycleId))") 
  List<Pitch> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.cycle.id = :cycleId AND p.deletedAt IS NULL")
  long countByCycleIdNotDeleted(@Param("cycleId") Long cycleId);

  @Query("SELECT p FROM Pitch p WHERE p.team.id = :teamId AND p.deletedAt IS NULL")
  List<Pitch> findByTeamIdNotDeleted(@Param("teamId") Long teamId);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status = :status AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusNotDeleted(@Param("cycleId") Long cycleId, @Param("status") PitchStatus status);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusNotIn(@Param("cycleId") Long cycleId,
      @Param("statuses") List<PitchStatus> statuses);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses")
  List<Pitch> findByCycleIdAndStatusNotInIncludingDeleted(@Param("cycleId") Long cycleId,
      @Param("statuses") List<PitchStatus> statuses);

  // Circuit Breaker queries
  @Query("SELECT p FROM Pitch p WHERE p.isCircuitBreakerTriggered = true AND p.deletedAt IS NULL")
  List<Pitch> findByIsCircuitBreakerTriggeredTrueNotDeleted();

  List<Pitch> findByIsCircuitBreakerTriggeredTrue();

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.isCircuitBreakerTriggered = true AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrueNotDeleted(Long cycleId);

  List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrue(Long cycleId);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status IN :statuses AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusInNotDeleted(@Param("cycleId") Long cycleId,
      @Param("statuses") List<PitchStatus> statuses);

  List<Pitch> findByCycleIdAndStatusIn(Long cycleId, List<PitchStatus> statuses);

  // Count methods for phase transition validation
  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status = :status AND p.deletedAt IS NULL")
  long countByCycleIdAndStatus(@Param("cycleId") Long cycleId, @Param("status") PitchStatus status);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status IN :statuses AND p.deletedAt IS NULL")
  long countByCycleIdAndStatusIn(@Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);

  @Query("SELECT p FROM Pitch p WHERE p.status = :status AND p.deletedAt IS NULL")
  List<Pitch> findByStatusNotDeleted(@Param("status") PitchStatus status);

  @Query("SELECT p FROM Pitch p WHERE p.status = :status AND p.cycle.id IN :cycleIds AND p.deletedAt IS NULL")
  List<Pitch> findByStatusAndCycleIdInNotDeleted(@Param("status") PitchStatus status,
      @Param("cycleIds") java.util.Set<Long> cycleIds);

  /**
   * Find all pitches for projects accessible to a user. Access is granted
   * through: 1. Project ownership 2. Direct project assignment (user_projects) 3.
   * Team membership on a cycle within the project.
   *
   * Uses LEFT JOIN on cycle and epic so pre-cycle pitches (IDEA/DRAFT/SHAPED with
   * no cycle assigned) are included when they belong to an accessible project
   * via their epic link.
   */
  @Query("""
      SELECT DISTINCT p FROM Pitch p
      LEFT JOIN p.cycle c
      LEFT JOIN c.project cproj
      LEFT JOIN p.epic e
      LEFT JOIN e.project eproj
      WHERE p.deletedAt IS NULL
      AND (
          cproj.id IN (
              SELECT DISTINCT prj.id FROM Project prj
              WHERE prj.owner.id = :userId
              OR prj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
              OR prj.id IN (
                  SELECT DISTINCT p2.cycle.project.id
                  FROM Pitch p2
                  JOIN p2.team t
                  JOIN TeamAssignment ta ON ta.team.id = t.id
                  JOIN ta.person per
                  JOIN User u ON u.person.id = per.id
                  WHERE u.id = :userId AND p2.cycle IS NOT NULL AND p2.deletedAt IS NULL
              )
          )
          OR eproj.id IN (
              SELECT DISTINCT prj.id FROM Project prj
              WHERE prj.owner.id = :userId
              OR prj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          )
      )
      ORDER BY p.id DESC
      """)
  List<Pitch> findAccessiblePitchesByUserId(@Param("userId") Long userId);

  /**
   * Find all pitches for projects accessible to a user including deleted ones.
   */
  @Query("""
      SELECT DISTINCT p FROM Pitch p
      JOIN FETCH p.cycle c
      JOIN c.project proj
      WHERE proj.id IN (
          SELECT DISTINCT prj.id FROM Project prj
          WHERE prj.owner.id = :userId
          OR prj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR prj.id IN (
              SELECT DISTINCT p2.cycle.project.id
              FROM Pitch p2
              JOIN p2.team t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId AND p2.cycle IS NOT NULL AND p2.deletedAt IS NULL
          )
      )
      ORDER BY p.id DESC
      """)
  List<Pitch> findAccessiblePitchesByUserIdIncludingDeleted(@Param("userId") Long userId);

  // Release-related queries
  @Query("SELECT p FROM Pitch p WHERE p.targetRelease.id = :releaseId AND p.deletedAt IS NULL")
  List<Pitch> findByTargetReleaseIdNotDeleted(@Param("releaseId") Long releaseId);

  @Query("SELECT p FROM Pitch p WHERE p.epic.id = :epicId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC, p.id ASC")
  List<Pitch> findByEpicIdNotDeleted(@Param("epicId") Long epicId);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.epic.id = :epicId AND p.deletedAt IS NULL")
  long countByEpicIdNotDeleted(@Param("epicId") Long epicId);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.epic.id = :epicId AND p.status = :status AND p.deletedAt IS NULL")
  long countByEpicIdAndStatusNotDeleted(@Param("epicId") Long epicId, @Param("status") PitchStatus status);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.targetRelease.id = :releaseId AND p.deletedAt IS NULL")
  long countByTargetReleaseIdNotDeleted(@Param("releaseId") Long releaseId);

  @Query("SELECT COUNT(p) FROM Pitch p WHERE p.targetRelease.id = :releaseId AND p.status = :status AND p.deletedAt IS NULL")
  long countByTargetReleaseIdAndStatusNotDeleted(@Param("releaseId") Long releaseId, @Param("status") PitchStatus status);

  @Query("SELECT p.targetRelease.id, COUNT(p) FROM Pitch p WHERE p.targetRelease.id IN :releaseIds AND p.deletedAt IS NULL GROUP BY p.targetRelease.id")
  List<Object[]> countByTargetReleaseIdsNotDeleted(@Param("releaseIds") List<Long> releaseIds);

  @Query("SELECT p.targetRelease.id, COUNT(p) FROM Pitch p WHERE p.targetRelease.id IN :releaseIds AND p.status = :status AND p.deletedAt IS NULL GROUP BY p.targetRelease.id")
  List<Object[]> countByTargetReleaseIdsAndStatusNotDeleted(@Param("releaseIds") List<Long> releaseIds, @Param("status") PitchStatus status);

  // ===== Ideas Pool Queries (IDEA status, no cycle) =====

  /** Find all ideas (raw concepts not yet being shaped). */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'IDEA' AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
  List<Pitch> findAllIdeas();

  /** Find ideas for a specific project via their epic's project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT p FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'IDEA' AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
  List<Pitch> findIdeasByProjectId(@Param("projectId") Long projectId);

  /** Find ideas for a specific epic. */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'IDEA' AND p.epic.id = :epicId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
  List<Pitch> findIdeasByEpicId(@Param("epicId") Long epicId);

  // ===== Shaping Pool Queries (DRAFT status, no cycle) =====

  /** Find all drafts (pitches being shaped). */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'DRAFT' AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findAllDrafts();

  /** Find drafts for a specific project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT p FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'DRAFT' AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findDraftsByProjectId(@Param("projectId") Long projectId);

  /** Find drafts for a specific epic. */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'DRAFT' AND p.epic.id = :epicId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findDraftsByEpicId(@Param("epicId") Long epicId);

  // ===== Betting Table Queries (SHAPED status, no cycle) =====

  /** Find all betting candidates (shaped pitches ready for cycle assignment). */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'SHAPED' AND p.cycle IS NULL AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findBettingCandidates();

  /** Find betting candidates for a specific project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT p FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'SHAPED' AND p.cycle IS NULL AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findBettingCandidatesByProjectId(@Param("projectId") Long projectId);

  /** Find betting candidates for a specific epic. */
  @Query("SELECT p FROM Pitch p WHERE p.status = 'SHAPED' AND p.cycle IS NULL AND p.epic.id = :epicId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
  List<Pitch> findBettingCandidatesByEpicId(@Param("epicId") Long epicId);

  // ===== Combined Pre-Cycle Queries =====

  /** Find all unassigned pitches (IDEA, DRAFT, or SHAPED without cycle). */
  @Query("SELECT p FROM Pitch p WHERE p.cycle IS NULL AND p.deletedAt IS NULL ORDER BY p.status, p.updatedAt DESC")
  List<Pitch> findAllUnassigned();

  /** Find unassigned pitches for a specific project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT p FROM Pitch p LEFT JOIN p.epic e WHERE p.cycle IS NULL AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL ORDER BY p.status, p.updatedAt DESC")
  List<Pitch> findUnassignedByProjectId(@Param("projectId") Long projectId);

  /** Find unassigned pitches for a specific epic. */
  @Query("SELECT p FROM Pitch p WHERE p.cycle IS NULL AND p.epic.id = :epicId AND p.deletedAt IS NULL ORDER BY p.status, p.updatedAt DESC")
  List<Pitch> findUnassignedByEpicId(@Param("epicId") Long epicId);

  /** Count ideas for a project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT COUNT(p) FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'IDEA' AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL")
  long countIdeasByProjectId(@Param("projectId") Long projectId);

  /** Count drafts for a project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT COUNT(p) FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'DRAFT' AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL")
  long countDraftsByProjectId(@Param("projectId") Long projectId);

  /** Count betting candidates for a project. Uses LEFT JOIN to include pitches with no epic. */
  @Query("SELECT COUNT(p) FROM Pitch p LEFT JOIN p.epic e WHERE p.status = 'SHAPED' AND p.cycle IS NULL AND (p.epic IS NULL OR e.project.id = :projectId) AND p.deletedAt IS NULL")
  long countBettingCandidatesByProjectId(@Param("projectId") Long projectId);
}
