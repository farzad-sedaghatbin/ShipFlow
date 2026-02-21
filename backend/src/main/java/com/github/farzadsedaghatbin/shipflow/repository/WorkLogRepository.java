package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
  // Non-pageable variants kept for internal aggregation use (BettingTableService, etc.)
  List<WorkLog> findByPitchId(Long pitchId);

  List<WorkLog> findByTaskId(Long taskId);

  List<WorkLog> findByPersonId(Long personId);

  List<WorkLog> findByPersonIdAndDate(Long personId, LocalDate date);

  List<WorkLog> findByPitchIdAndPersonId(Long pitchId, Long personId);

  List<WorkLog> findByTaskIdAndPersonId(Long taskId, Long personId);

  @Query("SELECT w FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)")
  List<WorkLog> findByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT w FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE w.person.id = :personId AND (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)")
  List<WorkLog> findByPersonIdAndCycleId(@Param("personId") Long personId, @Param("cycleId") Long cycleId);

  // Pageable variants for API endpoints
  Page<WorkLog> findByPitchId(Long pitchId, Pageable pageable);

  Page<WorkLog> findByTaskId(Long taskId, Pageable pageable);

  Page<WorkLog> findByPersonId(Long personId, Pageable pageable);

  Page<WorkLog> findByPersonIdAndDate(Long personId, LocalDate date, Pageable pageable);

  @Query(value = "SELECT w FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)",
         countQuery = "SELECT COUNT(w) FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)")
  Page<WorkLog> findByCycleId(@Param("cycleId") Long cycleId, Pageable pageable);

  @Query(value = "SELECT w FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE w.person.id = :personId AND (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)",
         countQuery = "SELECT COUNT(w) FROM WorkLog w LEFT JOIN w.pitch p LEFT JOIN w.task t WHERE w.person.id = :personId AND (p.cycle.id = :cycleId OR t.cycle.id = :cycleId)")
  Page<WorkLog> findByPersonIdAndCycleId(@Param("personId") Long personId, @Param("cycleId") Long cycleId, Pageable pageable);

  @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.id = :pitchId")
  Double getTotalHoursByPitchId(@Param("pitchId") Long pitchId);

  @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.task.id = :taskId")
  Double getTotalHoursByTaskId(@Param("taskId") Long taskId);

  @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.person.id = :personId")
  Double getTotalHoursByPersonId(@Param("personId") Long personId);

  /**
   * Batch query to get total hours for multiple pitches at once. Returns a list
   * of Object[] where [0] is pitchId and [1] is total hours.
   */
  @Query("SELECT w.pitch.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.id IN :pitchIds GROUP BY w.pitch.id")
  List<Object[]> getTotalHoursByPitchIds(@Param("pitchIds") List<Long> pitchIds);

  /**
   * Batch query to get total hours for multiple tasks at once. Returns a list of
   * Object[] where [0] is taskId and [1] is total hours.
   */
  @Query("SELECT w.task.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.task.id IN :taskIds GROUP BY w.task.id")
  List<Object[]> getTotalHoursByTaskIds(@Param("taskIds") List<Long> taskIds);

  /**
   * Get total hours for all pitches in a cycle. Returns a list of Object[] where
   * [0] is pitchId and [1] is total hours.
   */
  @Query("SELECT w.pitch.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.cycle.id = :cycleId GROUP BY w.pitch.id")
  List<Object[]> getTotalHoursByCycleId(@Param("cycleId") Long cycleId);
}
