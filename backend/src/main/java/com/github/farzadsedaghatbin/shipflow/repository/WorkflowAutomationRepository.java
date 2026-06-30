package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowAutomationRepository extends JpaRepository<WorkflowAutomation, Long> {

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.project.id = :projectId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
  List<WorkflowAutomation> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.project.id = :projectId AND a.enabled = true AND a.deletedAt IS NULL AND a.triggerType = :triggerType")
  List<WorkflowAutomation> findEnabledByProjectIdAndTriggerType(
      @Param("projectId") Long projectId, @Param("triggerType") TriggerType triggerType);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.enabled = true AND a.deletedAt IS NULL AND a.triggerType = :triggerType")
  List<WorkflowAutomation> findEnabledByTriggerType(@Param("triggerType") TriggerType triggerType);

  @Query("SELECT a FROM WorkflowAutomation a WHERE a.id = :id AND a.deletedAt IS NULL")
  Optional<WorkflowAutomation> findByIdNotDeleted(@Param("id") Long id);
}
