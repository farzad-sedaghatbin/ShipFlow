package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {
  List<Pitch> findByCycleId(Long cycleId);

  List<Pitch> findByTeamId(Long teamId);

  List<Pitch> findByCycleIdAndStatus(Long cycleId, PitchStatus status);

  @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses")
  List<Pitch> findByCycleIdAndStatusNotIn(
      @Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);

  // Circuit Breaker queries
  List<Pitch> findByIsCircuitBreakerTriggeredTrue();

  List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrue(Long cycleId);

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
  List<Pitch> findAccessiblePitchesByUserId(@Param("userId") Long userId);
}
