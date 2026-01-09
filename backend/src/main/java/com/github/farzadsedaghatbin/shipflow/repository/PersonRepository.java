package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    
    Optional<Person> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<Person> findByIsActiveTrue();
    
    List<Person> findByIsActiveFalse();
    
    @Query("SELECT p FROM Person p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByNameOrEmail(@Param("query") String query);
    
    @Query("SELECT DISTINCT p FROM Person p JOIN p.teamAssignments a WHERE a.team.id = :teamId AND a.isActive = true")
    List<Person> findByCurrentTeam(@Param("teamId") Long teamId);
    
    @Query("SELECT DISTINCT p FROM Person p JOIN p.teamAssignments a WHERE a.team.cycle.id = :cycleId AND a.isActive = true")
    List<Person> findByCurrentCycle(@Param("cycleId") Long cycleId);
    
    @Query("SELECT p FROM Person p WHERE p.skills LIKE %:skill%")
    List<Person> findBySkill(@Param("skill") String skill);
}
