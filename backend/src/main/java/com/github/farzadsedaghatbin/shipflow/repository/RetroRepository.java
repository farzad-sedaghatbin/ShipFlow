package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroRepository extends JpaRepository<Retrospective, Long> {

  List<Retrospective> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<Retrospective> findByCycleIdOrderByCreatedAtDesc(Long cycleId);

  List<Retrospective> findByCycleIdAndStatus(Long cycleId, RetroStatus status);

  List<Retrospective> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, RetroStatus status);

  @Query("SELECT COUNT(r) FROM Retrospective r WHERE r.cycle.id = :cycleId AND r.status = :status")
  long countByCycleIdAndStatus(@Param("cycleId") Long cycleId, @Param("status") RetroStatus status);

  @Query("SELECT COUNT(r) FROM Retrospective r WHERE r.cycle.id = :cycleId")
  long countByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT r FROM Retrospective r LEFT JOIN FETCH r.cycle LEFT JOIN FETCH r.project LEFT JOIN FETCH r.createdBy WHERE r.id = :id")
  Optional<Retrospective> findByIdWithDetails(@Param("id") Long id);

  @Query("SELECT r FROM Retrospective r LEFT JOIN FETCH r.items WHERE r.id = :id")
  Optional<Retrospective> findByIdWithItems(@Param("id") Long id);

  boolean existsByCycleIdAndStatus(Long cycleId, RetroStatus status);
}
