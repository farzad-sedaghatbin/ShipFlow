package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCycleId(Long cycleId);
    
    @Query("SELECT t FROM Team t " +
           "LEFT JOIN FETCH t.assignments a " +
           "LEFT JOIN FETCH t.cycle c " +
           "LEFT JOIN FETCH c.project")
    List<Team> findAllWithAssignments();
    
    @Query("SELECT t FROM Team t " +
           "LEFT JOIN FETCH t.assignments a " +
           "LEFT JOIN FETCH t.cycle c " +
           "LEFT JOIN FETCH c.project " +
           "WHERE t.id = :id")
    Optional<Team> findByIdWithAssignments(Long id);
}
