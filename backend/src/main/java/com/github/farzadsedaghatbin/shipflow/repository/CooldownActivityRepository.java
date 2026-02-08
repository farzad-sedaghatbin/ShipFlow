package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CooldownActivity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CooldownActivityRepository extends JpaRepository<CooldownActivity, Long> {

  /** Find all activities for a cycle ordered by priority */
  List<CooldownActivity> findByCycleIdOrderByPriorityAscCreatedAtDesc(Long cycleId);

  /** Find activities by cycle and status */
  List<CooldownActivity> findByCycleIdAndStatusOrderByPriorityAsc(Long cycleId, CooldownActivityStatus status);

  /** Find activities by cycle and type */
  List<CooldownActivity> findByCycleIdAndActivityTypeOrderByPriorityAsc(Long cycleId, CooldownActivityType type);

  /** Find activities assigned to a user */
  List<CooldownActivity> findByAssigneeIdOrderByPriorityAscCreatedAtDesc(Long userId);

  /** Find activities assigned to a user in a specific cycle */
  List<CooldownActivity> findByCycleIdAndAssigneeIdOrderByPriorityAsc(Long cycleId, Long userId);

  /** Count activities by status for a cycle */
  long countByCycleIdAndStatus(Long cycleId, CooldownActivityStatus status);

  /** Count activities by type for a cycle */
  long countByCycleIdAndActivityType(Long cycleId, CooldownActivityType type);

  /** Find incomplete activities (planned or in progress) */
  @Query("SELECT ca FROM CooldownActivity ca WHERE ca.cycle.id = :cycleId " +
         "AND ca.status IN ('PLANNED', 'IN_PROGRESS') ORDER BY ca.priority ASC")
  List<CooldownActivity> findIncompleteActivities(@Param("cycleId") Long cycleId);

  /** Find activities related to a specific pitch */
  List<CooldownActivity> findByRelatedPitchIdOrderByCreatedAtDesc(Long pitchId);

  /** Get activity type breakdown for a cycle */
  @Query("SELECT ca.activityType, COUNT(ca) FROM CooldownActivity ca " +
         "WHERE ca.cycle.id = :cycleId GROUP BY ca.activityType")
  List<Object[]> countActivitiesByTypeForCycle(@Param("cycleId") Long cycleId);

  /** Get status breakdown for a cycle */
  @Query("SELECT ca.status, COUNT(ca) FROM CooldownActivity ca " +
         "WHERE ca.cycle.id = :cycleId GROUP BY ca.status")
  List<Object[]> countActivitiesByStatusForCycle(@Param("cycleId") Long cycleId);

  /** Sum estimated hours for a cycle */
  @Query("SELECT COALESCE(SUM(ca.estimatedHours), 0) FROM CooldownActivity ca WHERE ca.cycle.id = :cycleId")
  int sumEstimatedHoursByCycleId(@Param("cycleId") Long cycleId);

  /** Sum actual hours for completed activities in a cycle */
  @Query("SELECT COALESCE(SUM(ca.actualHours), 0) FROM CooldownActivity ca " +
         "WHERE ca.cycle.id = :cycleId AND ca.status = 'COMPLETED'")
  int sumActualHoursForCompletedByCycleId(@Param("cycleId") Long cycleId);

  /** Check if cycle has any activities */
  boolean existsByCycleId(Long cycleId);
}
