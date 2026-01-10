package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findByPitchId(Long pitchId);
    List<WorkLog> findByTaskId(Long taskId);
    List<WorkLog> findByPersonId(Long personId);
    List<WorkLog> findByPersonIdAndDate(Long personId, LocalDate date);
    List<WorkLog> findByPitchIdAndPersonId(Long pitchId, Long personId);
    List<WorkLog> findByTaskIdAndPersonId(Long taskId, Long personId);
    
    @Query("SELECT w FROM WorkLog w WHERE w.pitch.cycle.id = :cycleId OR w.task.cycle.id = :cycleId")
    List<WorkLog> findByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT w FROM WorkLog w WHERE w.person.id = :personId AND (w.pitch.cycle.id = :cycleId OR w.task.cycle.id = :cycleId)")
    List<WorkLog> findByPersonIdAndCycleId(@Param("personId") Long personId, @Param("cycleId") Long cycleId);
    
    @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.id = :pitchId")
    Double getTotalHoursByPitchId(@Param("pitchId") Long pitchId);
    
    @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.task.id = :taskId")
    Double getTotalHoursByTaskId(@Param("taskId") Long taskId);
    
    @Query("SELECT SUM(w.hoursSpent) FROM WorkLog w WHERE w.person.id = :personId")
    Double getTotalHoursByPersonId(@Param("personId") Long personId);

    /**
     * Batch query to get total hours for multiple pitches at once.
     * Returns a list of Object[] where [0] is pitchId and [1] is total hours.
     */
    @Query("SELECT w.pitch.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.id IN :pitchIds GROUP BY w.pitch.id")
    List<Object[]> getTotalHoursByPitchIds(@Param("pitchIds") List<Long> pitchIds);
    
    /**
     * Batch query to get total hours for multiple tasks at once.
     * Returns a list of Object[] where [0] is taskId and [1] is total hours.
     */
    @Query("SELECT w.task.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.task.id IN :taskIds GROUP BY w.task.id")
    List<Object[]> getTotalHoursByTaskIds(@Param("taskIds") List<Long> taskIds);

    /**
     * Get total hours for all pitches in a cycle.
     * Returns a list of Object[] where [0] is pitchId and [1] is total hours.
     */
    @Query("SELECT w.pitch.id, SUM(w.hoursSpent) FROM WorkLog w WHERE w.pitch.cycle.id = :cycleId GROUP BY w.pitch.id")
    List<Object[]> getTotalHoursByCycleId(@Param("cycleId") Long cycleId);
}
