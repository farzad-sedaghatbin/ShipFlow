package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
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

  /**
   * Runs one SCHEDULE automation for the slot it was claimed for. Synchronous: it is called from
   * {@code WorkflowScheduler}'s own thread, and the run — including its execution record — is
   * finished before the next due automation starts.
   */
  public void runScheduled(WorkflowAutomation automation, Instant slot) {
    Map<String, Object> context = new HashMap<>();
    context.put("trigger", TriggerType.SCHEDULE.name());
    context.put("scheduledFor", slot != null ? slot.toString() : "");
    if (automation.getProject() != null) {
      context.put("projectId", automation.getProject().getId());
    }
    execute(automation, TriggerType.SCHEDULE, buildEventData(context), context);
  }

  /**
   * "Run now" for an agent loop: same path and same execution record as a scheduled run, started by
   * a person. Returns the record so the caller can show what happened.
   */
  public WorkflowAutomationExecution runNow(WorkflowAutomation automation) {
    Map<String, Object> context = new HashMap<>();
    context.put("trigger", "MANUAL");
    if (automation.getProject() != null) {
      context.put("projectId", automation.getProject().getId());
    }
    return execute(automation, automation.getTriggerType(), buildEventData(context), context);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected WorkflowAutomationExecution execute(WorkflowAutomation automation, TriggerType triggerType,
      String eventData, Map<String, Object> context) {
    AutomationExecutionStatus status;
    String resultMessage;
    try {
      String detail = actionDispatcher.dispatch(automation, context);
      status = AutomationExecutionStatus.SUCCESS;
      resultMessage = detail != null && !detail.isBlank() ? detail : "Action executed successfully.";
      // Counted with a targeted UPDATE: saving this (detached) entity would write back a stale
      // next_run_at and undo the scheduler's claim on the slot.
      automationRepository.recordExecution(automation.getId(), LocalDateTime.now());
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
    return executionRepository.save(execution);
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
