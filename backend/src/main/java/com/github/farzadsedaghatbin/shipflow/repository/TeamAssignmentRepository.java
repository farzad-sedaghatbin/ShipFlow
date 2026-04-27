package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamAssignmentRepository extends JpaRepository<TeamAssignment, Long> {

  List<TeamAssignment> findByPersonId(Long personId);

  List<TeamAssignment> findByTeamId(Long teamId);

  List<TeamAssignment> findByPersonIdAndIsActiveTrue(Long personId);

  List<TeamAssignment> findByTeamIdAndIsActiveTrue(Long teamId);

  @Query("SELECT ta FROM TeamAssignment ta WHERE ta.person.id = :personId AND ta.team.id = :teamId AND ta.isActive = true")
  Optional<TeamAssignment> findActiveByPersonAndTeam(@Param("personId") Long personId, @Param("teamId") Long teamId);

  @Query("SELECT DISTINCT ta FROM TeamAssignment ta JOIN ta.team t JOIN t.pitches p WHERE ta.person.id = :personId AND p.cycle.id = :cycleId AND p.deletedAt IS NULL")
  List<TeamAssignment> findByPersonAndCycle(@Param("personId") Long personId, @Param("cycleId") Long cycleId);

  @Query("SELECT DISTINCT ta FROM TeamAssignment ta JOIN ta.team t JOIN t.pitches p WHERE p.cycle.id = :cycleId AND ta.isActive = true AND p.deletedAt IS NULL")
  List<TeamAssignment> findActiveByCycle(@Param("cycleId") Long cycleId);

  List<TeamAssignment> findByRole(TeamMemberRole role);

  @Query("SELECT ta FROM TeamAssignment ta WHERE ta.startDate <= :date AND (ta.endDate IS NULL OR ta.endDate >= :date)")
  List<TeamAssignment> findActiveOnDate(@Param("date") LocalDate date);

  @Query("SELECT COUNT(ta) > 0 FROM TeamAssignment ta WHERE ta.person.id = :personId AND ta.team.id = :teamId AND ta.isActive = true")
  boolean existsActiveAssignment(@Param("personId") Long personId, @Param("teamId") Long teamId);

  @Query("SELECT ta FROM TeamAssignment ta JOIN FETCH ta.team t WHERE ta.person.id = :personId ORDER BY ta.startDate DESC")
  List<TeamAssignment> findByPersonIdWithTeamAndCycle(@Param("personId") Long personId);
}
