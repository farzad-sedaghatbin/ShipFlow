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
  
  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId);
  
  @Query("SELECT p FROM Pitch p WHERE p.team.id = :teamId AND p.deletedAt IS NULL")
  List<Pitch> findByTeamIdNotDeleted(@Param("teamId") Long teamId);
  
  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status = :status AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusNotDeleted(@Param("cycleId") Long cycleId, @Param("status") PitchStatus status);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusNotIn(
      @Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses")
  List<Pitch> findByCycleIdAndStatusNotInIncludingDeleted(
      @Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);

  // Circuit Breaker queries
  @Query("SELECT p FROM Pitch p WHERE p.isCircuitBreakerTriggered = true AND p.deletedAt IS NULL")
  List<Pitch> findByIsCircuitBreakerTriggeredTrueNotDeleted();
  
  List<Pitch> findByIsCircuitBreakerTriggeredTrue();

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.isCircuitBreakerTriggered = true AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrueNotDeleted(Long cycleId);
  
  List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrue(Long cycleId);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status IN :statuses AND p.deletedAt IS NULL")
  List<Pitch> findByCycleIdAndStatusInNotDeleted(@Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);
  
  List<Pitch> findByCycleIdAndStatusIn(Long cycleId, List<PitchStatus> statuses);

  /**
   * Find all pitches for projects accessible to a user. Access is granted through: 1. Project
   * ownership 2. Direct project assignment (user_projects) 3. Team membership
   */
  @Query(
      """
        SELECT DISTINCT p FROM Pitch p
        JOIN FETCH p.cycle c
        JOIN c.project proj
        WHERE p.deletedAt IS NULL AND proj.id IN (
            SELECT DISTINCT prj.id FROM Project prj
            WHERE prj.owner.id = :userId
            OR prj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
            OR prj.id IN (
                SELECT DISTINCT cyc.project.id
                FROM Cycle cyc
                JOIN cyc.teams t
                JOIN TeamAssignment ta ON ta.team.id = t.id
                JOIN ta.person per
                JOIN User u ON u.person.id = per.id
                WHERE u.id = :userId
            )
        )
        ORDER BY p.id DESC
        """)
  List<Pitch> findAccessiblePitchesByUserId(@Param("userId") Long userId);
  
  /**
   * Find all pitches for projects accessible to a user including deleted ones.
   */
  @Query(
      """
        SELECT DISTINCT p FROM Pitch p
        JOIN FETCH p.cycle c
        JOIN c.project proj
        WHERE proj.id IN (
            SELECT DISTINCT prj.id FROM Project prj
            WHERE prj.owner.id = :userId
            OR prj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
            OR prj.id IN (
                SELECT DISTINCT cyc.project.id
                FROM Cycle cyc
                JOIN cyc.teams t
                JOIN TeamAssignment ta ON ta.team.id = t.id
                JOIN ta.person per
                JOIN User u ON u.person.id = per.id
                WHERE u.id = :userId
            )
        )
        ORDER BY p.id DESC
        """)
  List<Pitch> findAccessiblePitchesByUserIdIncludingDeleted(@Param("userId") Long userId);
}
