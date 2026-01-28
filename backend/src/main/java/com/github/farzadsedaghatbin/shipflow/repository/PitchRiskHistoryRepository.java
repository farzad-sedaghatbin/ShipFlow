package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PitchRiskHistoryRepository extends JpaRepository<PitchRiskHistory, Long> {

    /**
     * Find all risk history for a specific pitch, ordered by date descending.
     */
    List<PitchRiskHistory> findByPitchIdOrderByRecordedAtDesc(Long pitchId);

    /**
     * Find risk history for a pitch within a date range.
     */
    @Query("SELECT h FROM PitchRiskHistory h WHERE h.pitch.id = :pitchId " +
           "AND h.recordedAt >= :startDate AND h.recordedAt <= :endDate " +
           "ORDER BY h.recordedAt ASC")
    List<PitchRiskHistory> findByPitchIdAndDateRange(
        @Param("pitchId") Long pitchId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find the most recent risk history entry for a pitch.
     */
    Optional<PitchRiskHistory> findFirstByPitchIdOrderByRecordedAtDesc(Long pitchId);

    /**
     * Find all pitches with risk history recorded today.
     */
    @Query("SELECT DISTINCT h.pitch.id FROM PitchRiskHistory h " +
           "WHERE h.recordedAt >= :startOfDay AND h.recordedAt < :endOfDay")
    List<Long> findPitchIdsWithHistoryToday(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * Delete old risk history older than a certain date.
     * Useful for data retention policies.
     */
    void deleteByRecordedAtBefore(LocalDateTime date);
}
