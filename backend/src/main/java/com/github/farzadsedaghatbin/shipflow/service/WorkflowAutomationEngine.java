package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core trigger/action engine for workflow automations.
 * Resolves which rules match an event, dispatches actions, and records execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowAutomationEngine {

  private final WorkflowAutomationRepository automationRepository;
  private final WorkflowAutomationExecutionRepository executionRepository;
  private final WorkflowAutomationActionDispatcher actionDispatcher;

  /**
   * Fire all enabled automations that match this trigger/project combination.
   * Runs asynchronously so it never delays the primary transaction.
   */
  @Async
  public void fire(TriggerType triggerType, Long projectId, Map<String, Object> context) {
    List<WorkflowAutomation> candidates = projectId != null
        ? automationRepository.findEnabledByProjectIdAndTriggerType(projectId, triggerType)
        : automationRepository.findEnabledByTriggerType(triggerType);

    if (candidates.isEmpty()) return;

    String eventData = buildEventData(context);
    log.debug("Firing {} automations for trigger={} project={}", candidates.size(), triggerType, projectId);

    for (WorkflowAutomation automation : candidates) {
      execute(automation, triggerType, eventData, context);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected void execute(WorkflowAutomation automation, TriggerType triggerType,
      String eventData, Map<String, Object> context) {
    AutomationExecutionStatus status;
    String resultMessage;
    try {
      actionDispatcher.dispatch(automation, context);
      status = AutomationExecutionStatus.SUCCESS;
      resultMessage = "Action executed successfully.";
      automation.setExecutionCount(automation.getExecutionCount() + 1);
      automation.setLastTriggeredAt(LocalDateTime.now());
      automationRepository.save(automation);
    } catch (Exception ex) {
      status = AutomationExecutionStatus.FAILURE;
      resultMessage = "Action failed: " + ex.getMessage();
      log.warn("Automation id={} '{}' failed for trigger {}: {}", automation.getId(), automation.getName(), triggerType, ex.getMessage());
    }

    WorkflowAutomationExecution execution = WorkflowAutomationExecution.builder()
        .automation(automation)
        .triggerEventType(triggerType.name())
        .triggerEventData(eventData)
        .status(status)
        .resultMessage(resultMessage)
        .build();
    executionRepository.save(execution);
  }

  private String buildEventData(Map<String, Object> context) {
    if (context == null || context.isEmpty()) return "{}";
    StringBuilder sb = new StringBuilder("{");
    context.forEach((k, v) -> sb.append('"').append(k).append("\":\"").append(v).append("\","));
    if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
    sb.append('}');
    return sb.toString();
  }
}
