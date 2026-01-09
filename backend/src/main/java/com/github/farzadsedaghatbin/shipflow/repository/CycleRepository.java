package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
