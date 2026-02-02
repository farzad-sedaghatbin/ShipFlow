package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchRiskHistoryRepository extends JpaRepository<PitchRiskHistory, Long> {

  /**
   * Find all risk history for a specific pitch, ordered by date descending. Eagerly fetches the
   * pitch and deletedBy to avoid lazy initialization issues.
   */
  @Query(
      "SELECT h FROM PitchRiskHistory h LEFT JOIN FETCH h.pitch p LEFT JOIN FETCH p.deletedBy WHERE p.id = :pitchId ORDER BY h.recordedAt DESC")
  List<PitchRiskHistory> findByPitchIdOrderByRecordedAtDesc(@Param("pitchId") Long pitchId);

  /**
   * Find risk history for a pitch within a date range. Eagerly fetches the pitch and deletedBy to avoid lazy
   * initialization issues.
   */
  @Query(
      "SELECT h FROM PitchRiskHistory h LEFT JOIN FETCH h.pitch p LEFT JOIN FETCH p.deletedBy WHERE p.id = :pitchId "
          + "AND h.recordedAt >= :startDate AND h.recordedAt <= :endDate "
          + "ORDER BY h.recordedAt ASC")
  List<PitchRiskHistory> findByPitchIdAndDateRange(
      @Param("pitchId") Long pitchId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  /**
   * Find the most recent risk history entry for a pitch. Eagerly fetches the pitch and deletedBy to avoid lazy
   * initialization issues.
   */
  @Query(
      "SELECT h FROM PitchRiskHistory h LEFT JOIN FETCH h.pitch p LEFT JOIN FETCH p.deletedBy WHERE p.id = :pitchId ORDER BY h.recordedAt DESC LIMIT 1")
  Optional<PitchRiskHistory> findFirstByPitchIdOrderByRecordedAtDesc(
      @Param("pitchId") Long pitchId);

  /** Find all pitches with risk history recorded today. */
  @Query(
      "SELECT DISTINCT h.pitch.id FROM PitchRiskHistory h "
          + "WHERE h.recordedAt >= :startOfDay AND h.recordedAt < :endOfDay")
  List<Long> findPitchIdsWithHistoryToday(
      @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

  /** Delete old risk history older than a certain date. Useful for data retention policies. */
  void deleteByRecordedAtBefore(LocalDateTime date);
}
