package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {
    List<Pitch> findByCycleId(Long cycleId);
    List<Pitch> findByTeamId(Long teamId);
    List<Pitch> findByCycleIdAndStatus(Long cycleId, PitchStatus status);
    
    @Query("SELECT p FROM Pitch p WHERE p.cycle.id = :cycleId AND p.status NOT IN :statuses")
    List<Pitch> findByCycleIdAndStatusNotIn(@Param("cycleId") Long cycleId, @Param("statuses") List<PitchStatus> statuses);
    
    // Circuit Breaker queries
    List<Pitch> findByIsCircuitBreakerTriggeredTrue();
    List<Pitch> findByCycleIdAndIsCircuitBreakerTriggeredTrue(Long cycleId);
    List<Pitch> findByCycleIdAndStatusIn(Long cycleId, List<PitchStatus> statuses);
}
