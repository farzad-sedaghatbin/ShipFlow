package com.github.farzadsedaghatbin.shipflow.dto.workflow;

import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowAutomationExecutionDto {
  private Long id;
  private Long automationId;
  private String automationName;
  private String triggerEventType;
  private String triggerEventData;
  private AutomationExecutionStatus status;
  private String resultMessage;
  private LocalDateTime executedAt;
}
