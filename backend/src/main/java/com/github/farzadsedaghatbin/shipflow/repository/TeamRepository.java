package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

  /** Find teams that have at least one pitch assigned to the given cycle. */
  @Query("SELECT DISTINCT t FROM Team t JOIN t.pitches p WHERE p.cycle.id = :cycleId AND p.deletedAt IS NULL")
  List<Team> findByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT t FROM Team t LEFT JOIN FETCH t.assignments a")
  List<Team> findAllWithAssignments();

  @Query("SELECT t FROM Team t LEFT JOIN FETCH t.assignments a WHERE t.id = :id")
  Optional<Team> findByIdWithAssignments(@Param("id") Long id);
}
