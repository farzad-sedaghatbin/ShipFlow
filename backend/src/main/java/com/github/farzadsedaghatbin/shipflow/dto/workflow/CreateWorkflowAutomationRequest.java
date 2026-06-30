package com.github.farzadsedaghatbin.shipflow.dto.workflow;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateWorkflowAutomationRequest {

  @NotBlank
  private String name;

  private String description;

  @NotNull
  private Long projectId;

  @NotNull
  private TriggerType triggerType;

  private String triggerConfig;

  @NotNull
  private ActionType actionType;

  private String actionConfig;

  private boolean enabled = true;

  private Long templateId;
}
