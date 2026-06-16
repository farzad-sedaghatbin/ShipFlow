package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowAutomationExecutionRepository
    extends JpaRepository<WorkflowAutomationExecution, Long> {

  @Query("SELECT e FROM WorkflowAutomationExecution e WHERE e.automation.id = :automationId ORDER BY e.executedAt DESC")
  List<WorkflowAutomationExecution> findRecentByAutomationId(
      @Param("automationId") Long automationId, Pageable pageable);

  @Query("SELECT e FROM WorkflowAutomationExecution e WHERE e.automation.project.id = :projectId ORDER BY e.executedAt DESC")
  List<WorkflowAutomationExecution> findRecentByProjectId(
      @Param("projectId") Long projectId, Pageable pageable);

  long countByAutomationId(Long automationId);
}
