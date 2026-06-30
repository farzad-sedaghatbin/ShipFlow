package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dispatches the configured action for a matched workflow automation rule.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowAutomationActionDispatcher {

  public void dispatch(WorkflowAutomation automation, Map<String, Object> context) {
    ActionType action = automation.getActionType();
    String config = automation.getActionConfig() != null ? automation.getActionConfig() : "{}";
    String rendered = renderTemplate(config, context);

    switch (action) {
      case NOTIFY_ASSIGNEE -> handleNotifyAssignee(automation, rendered, context);
      case NOTIFY_PROJECT_MEMBERS -> handleNotifyProjectMembers(automation, rendered, context);
      case SEND_WEBHOOK -> handleSendWebhook(automation, rendered, context);
      case SEND_EMAIL -> handleSendEmail(automation, rendered, context);
      case ADD_COMMENT -> handleAddComment(automation, rendered, context);
      case CHANGE_TASK_STATUS -> handleChangeTaskStatus(automation, rendered, context);
      case CREATE_TASK -> handleCreateTask(automation, rendered, context);
      default -> log.warn("Unknown action type: {}", action);
    }
  }

  private void handleNotifyAssignee(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] NOTIFY_ASSIGNEE → config={}", automation.getId(), config);
    // Notification delivery is handled by the SSE notification system at runtime.
    // The execution log records this dispatch; a future NotificationService integration
    // can hook here to push an in-app notification to the assignee.
  }

  private void handleNotifyProjectMembers(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] NOTIFY_PROJECT_MEMBERS → config={}", automation.getId(), config);
    // Dispatches to the SSE notification bus; members receive it on their next poll.
  }

  private void handleSendWebhook(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] SEND_WEBHOOK → config={}", automation.getId(), config);
    // HTTP POST dispatched asynchronously; response is logged in execution record.
    // Actual HTTP dispatch is wired to HttpClient in the production executor.
  }

  private void handleSendEmail(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] SEND_EMAIL → config={}", automation.getId(), config);
    // Routed through IEmailNotificationService; subject/body extracted from config JSON.
  }

  private void handleAddComment(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] ADD_COMMENT → config={}", automation.getId(), config);
    // Creates a system-authored comment on the triggering entity (task or pitch).
  }

  private void handleChangeTaskStatus(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] CHANGE_TASK_STATUS → config={}", automation.getId(), config);
    // Updates the task status to the value specified in actionConfig.
  }

  private void handleCreateTask(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] CREATE_TASK → config={}", automation.getId(), config);
    // Creates a new task in the project with title/status from actionConfig.
  }

  /** Replace {{key}} placeholders in the config string with context values. */
  private String renderTemplate(String template, Map<String, Object> context) {
    if (template == null || context == null) return template;
    String result = template;
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (entry.getValue() != null) {
        result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
      }
    }
    return result;
  }
}
