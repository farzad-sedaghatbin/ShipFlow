package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for hill chart history entities.
 */
@Repository
public interface HillChartHistoryRepository extends JpaRepository<HillChartHistory, Long> {

    List<HillChartHistory> findByHillChartPointIdOrderByCreatedAtDesc(Long hillChartPointId);

    List<HillChartHistory> findByPitchIdOrderByCreatedAtDesc(Long pitchId);

    @Query("SELECT h FROM HillChartHistory h WHERE h.pitch.cycle.id = :cycleId ORDER BY h.createdAt DESC")
    List<HillChartHistory> findByCycleId(@Param("cycleId") Long cycleId);

    @Query("SELECT h FROM HillChartHistory h WHERE h.createdAt >= :startDate ORDER BY h.createdAt DESC")
    List<HillChartHistory> findRecentHistory(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(h.confidenceLevel) FROM HillChartHistory h WHERE h.pitch.id = :pitchId AND h.confidenceLevel IS NOT NULL")
    Double getAverageConfidenceByPitch(@Param("pitchId") Long pitchId);

    @Query("SELECT h FROM HillChartHistory h WHERE h.movedBy.id = :userId ORDER BY h.createdAt DESC")
    List<HillChartHistory> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(h) FROM HillChartHistory h WHERE h.hillChartPoint.id = :pointId")
    Long countMovesByPointId(@Param("pointId") Long pointId);
}
