package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CustomMetric;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomMetricRepository extends JpaRepository<CustomMetric, Long> {

  /** Find metric by ID with scope relationships eagerly loaded */
  @Query(
      "SELECT m FROM CustomMetric m "
          + "LEFT JOIN FETCH m.cycle "
          + "LEFT JOIN FETCH m.pitch "
          + "LEFT JOIN FETCH m.team "
          + "WHERE m.id = :id")
  Optional<CustomMetric> findById(@Param("id") Long id);

  /** Find all metrics for a specific user with scope relationships eagerly loaded */
  @Query(
      "SELECT DISTINCT m FROM CustomMetric m "
          + "LEFT JOIN FETCH m.cycle "
          + "LEFT JOIN FETCH m.pitch "
          + "LEFT JOIN FETCH m.team "
          + "WHERE m.user.id = :userId "
          + "ORDER BY m.name ASC")
  List<CustomMetric> findByUserIdOrderByNameAsc(@Param("userId") Long userId);

  /** Find all active metrics for a specific user with scope relationships eagerly loaded */
  @Query(
      "SELECT DISTINCT m FROM CustomMetric m "
          + "LEFT JOIN FETCH m.cycle "
          + "LEFT JOIN FETCH m.pitch "
          + "LEFT JOIN FETCH m.team "
          + "WHERE m.user.id = :userId AND m.isActive = true "
          + "ORDER BY m.name ASC")
  List<CustomMetric> findByUserIdAndIsActiveTrueOrderByNameAsc(@Param("userId") Long userId);

  /** Find a metric by name for a specific user with scope relationships eagerly loaded */
  @Query(
      "SELECT m FROM CustomMetric m "
          + "LEFT JOIN FETCH m.cycle "
          + "LEFT JOIN FETCH m.pitch "
          + "LEFT JOIN FETCH m.team "
          + "WHERE m.user.id = :userId AND m.name = :name")
  Optional<CustomMetric> findByUserIdAndName(
      @Param("userId") Long userId, @Param("name") String name);

  /** Check if a metric name exists for a user */
  boolean existsByUserIdAndName(Long userId, String name);

  /** Find metrics by data source with scope relationships eagerly loaded */
  @Query(
      "SELECT DISTINCT m FROM CustomMetric m "
          + "LEFT JOIN FETCH m.cycle "
          + "LEFT JOIN FETCH m.pitch "
          + "LEFT JOIN FETCH m.team "
          + "WHERE m.user.id = :userId AND m.dataSource = :dataSource")
  List<CustomMetric> findByUserIdAndDataSource(
      @Param("userId") Long userId, @Param("dataSource") CustomMetric.DataSource dataSource);

  /** Count active metrics for a user */
  @Query("SELECT COUNT(m) FROM CustomMetric m WHERE m.user.id = :userId AND m.isActive = true")
  long countActiveMetricsByUser(@Param("userId") Long userId);
}
