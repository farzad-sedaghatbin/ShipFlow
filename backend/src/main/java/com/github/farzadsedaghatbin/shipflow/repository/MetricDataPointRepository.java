package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.MetricDataPoint;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricDataPointRepository extends JpaRepository<MetricDataPoint, Long> {

  /** Find all data points for a metric, ordered by date descending */
  List<MetricDataPoint> findByMetricIdOrderByCalculationDateDesc(Long metricId);

  /** Find data points within a date range */
  @Query("SELECT mdp FROM MetricDataPoint mdp WHERE mdp.metric.id = :metricId "
      + "AND mdp.calculationDate BETWEEN :startDate AND :endDate " + "ORDER BY mdp.calculationDate DESC")
  List<MetricDataPoint> findByMetricIdAndDateRange(@Param("metricId") Long metricId,
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /** Get the latest data point for a metric */
  @Query("SELECT mdp FROM MetricDataPoint mdp WHERE mdp.metric.id = :metricId "
      + "ORDER BY mdp.calculationDate DESC LIMIT 1")
  MetricDataPoint findLatestByMetricId(@Param("metricId") Long metricId);

  /** Get the last N data points for a metric */
  @Query("SELECT mdp FROM MetricDataPoint mdp WHERE mdp.metric.id = :metricId "
      + "ORDER BY mdp.calculationDate DESC LIMIT :limit")
  List<MetricDataPoint> findTopNByMetricId(@Param("metricId") Long metricId, @Param("limit") int limit);

  /** Delete old data points (cleanup) */
  void deleteByCalculationDateBefore(LocalDateTime date);
}
