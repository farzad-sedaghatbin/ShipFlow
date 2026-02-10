package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HillChartPointRepository extends JpaRepository<HillChartPoint, Long> {
  List<HillChartPoint> findByPitchId(Long pitchId);

  List<HillChartPoint> findByPitchIdOrderByUpdatedAtDesc(Long pitchId);

  void deleteByPitchId(Long pitchId);

  @Query("SELECT h FROM HillChartPoint h WHERE LOWER(h.scope) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.description) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<HillChartPoint> searchHillChartPoints(@Param("query") String query);

  /**
   * Find the scope linked to a specific task.
   * Used for Scope-Task bridge synchronization.
   */
  Optional<HillChartPoint> findByLinkedTaskId(Long linkedTaskId);

  /**
   * Find all scopes that have auto-progress enabled.
   */
  List<HillChartPoint> findByAutoProgressEnabledTrue();

  /**
   * Find all scopes for a pitch that have linked tasks.
   */
  @Query("SELECT h FROM HillChartPoint h WHERE h.pitch.id = :pitchId AND h.linkedTask IS NOT NULL")
  List<HillChartPoint> findByPitchIdWithLinkedTasks(@Param("pitchId") Long pitchId);
}
