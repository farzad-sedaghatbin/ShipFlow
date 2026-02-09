package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {
  List<Cycle> findByIsActiveTrue();

  List<Cycle> findByIsActiveFalse();

  List<Cycle> findAllByOrderByStartDateDesc();

  // Project-specific queries
  List<Cycle> findByProjectIdOrderByStartDateDesc(Long projectId);

  List<Cycle> findByProjectIdAndIsActiveTrue(Long projectId);

  @Query("SELECT COUNT(c) FROM Cycle c WHERE c.project.id = :projectId")
  long countByProjectId(@Param("projectId") Long projectId);

  @Query("SELECT COUNT(c) FROM Cycle c WHERE c.project.id = :projectId AND c.isActive = true")
  long countActiveByProjectId(@Param("projectId") Long projectId);

  @Query("SELECT c FROM Cycle c LEFT JOIN FETCH c.project WHERE c.id = :id")
  java.util.Optional<Cycle> findByIdWithProject(@Param("id") Long id);

  /**
   * Find all cycles for projects accessible to a user. Access is granted through:
   * 1. Project ownership 2. Direct project assignment (user_projects) 3. Team
   * membership
   */
  @Query("""
      SELECT DISTINCT c FROM Cycle c
      JOIN FETCH c.project p
      WHERE p.id IN (
          SELECT DISTINCT proj.id FROM Project proj
          WHERE proj.owner.id = :userId
          OR proj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR proj.id IN (
              SELECT DISTINCT cyc.project.id
              FROM Cycle cyc
              JOIN cyc.teams t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId
          )
      )
      ORDER BY c.startDate DESC
      """)
  List<Cycle> findAccessibleCyclesByUserId(@Param("userId") Long userId);

  /** Find active cycles for accessible projects */
  @Query("""
      SELECT DISTINCT c FROM Cycle c
      JOIN FETCH c.project p
      WHERE c.isActive = true
      AND p.id IN (
          SELECT DISTINCT proj.id FROM Project proj
          WHERE proj.owner.id = :userId
          OR proj.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
          OR proj.id IN (
              SELECT DISTINCT cyc.project.id
              FROM Cycle cyc
              JOIN cyc.teams t
              JOIN TeamAssignment ta ON ta.team.id = t.id
              JOIN ta.person per
              JOIN User u ON u.person.id = per.id
              WHERE u.id = :userId
          )
      )
      ORDER BY c.startDate DESC
      """)
  List<Cycle> findAccessibleActiveCyclesByUserId(@Param("userId") Long userId);

  /**
   * Find the next upcoming cycle for a project (start date after today, ordered by start date).
   * Used for converting retro items to pitch drafts.
   */
  Optional<Cycle> findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(
      Long projectId, LocalDate currentDate);
}
