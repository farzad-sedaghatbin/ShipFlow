package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "workflow_automation_executions",
    indexes = {
      @Index(name = "idx_automation_exec_automation", columnList = "automation_id"),
      @Index(name = "idx_automation_exec_status", columnList = "status"),
      @Index(name = "idx_automation_exec_executed_at", columnList = "executed_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowAutomationExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "automation_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private WorkflowAutomation automation;

  @Column(name = "trigger_event_type", nullable = false, length = 100)
  private String triggerEventType;

  @Column(name = "trigger_event_data", columnDefinition = "TEXT")
  private String triggerEventData;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AutomationExecutionStatus status;

  @Column(name = "result_message", columnDefinition = "TEXT")
  private String resultMessage;

  @Column(name = "executed_at", nullable = false)
  private LocalDateTime executedAt;

  @PrePersist
  void prePersist() {
    if (executedAt == null) executedAt = LocalDateTime.now();
  }
}
