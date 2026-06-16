package com.github.farzadsedaghatbin.shipflow.dto.workflow;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowAutomationTemplateDto {
  private Long id;
  private String name;
  private String description;
  private String category;
  private TriggerType triggerType;
  private String defaultTriggerConfig;
  private ActionType actionType;
  private String defaultActionConfig;
  private String iconName;
  private boolean builtIn;
  private int sortOrder;
}
