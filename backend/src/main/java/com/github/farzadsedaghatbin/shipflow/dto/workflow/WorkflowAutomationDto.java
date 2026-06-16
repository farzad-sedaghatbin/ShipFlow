package com.github.farzadsedaghatbin.shipflow.dto.workflow;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowAutomationDto {
  private Long id;
  private String name;
  private String description;
  private Long projectId;
  private TriggerType triggerType;
  private String triggerConfig;
  private ActionType actionType;
  private String actionConfig;
  private boolean enabled;
  private Long templateId;
  private String templateName;
  private long executionCount;
  private LocalDateTime lastTriggeredAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
