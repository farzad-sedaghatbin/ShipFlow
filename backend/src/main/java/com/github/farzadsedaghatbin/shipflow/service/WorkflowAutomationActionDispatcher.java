package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dispatches the configured action for a matched workflow automation rule.
 *
 * <p>Only NOTIFY_ASSIGNEE and NOTIFY_PROJECT_MEMBERS actually deliver anything today (via
 * {@link DashboardNotificationService}'s in-app/SSE notification bus). The other five action
 * types (SEND_WEBHOOK, SEND_EMAIL, ADD_COMMENT, CHANGE_TASK_STATUS, CREATE_TASK) are recognized
 * and logged but intentionally NOT executed yet — see each handler's Javadoc for what real
 * execution would require. Tracked as a follow-up; do not assume they work from the presence of
 * a log line.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowAutomationActionDispatcher {

  private final TaskRepository taskRepository;
  private final UserProjectRepository userProjectRepository;
  private final DashboardNotificationService notificationService;

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

  /** Extracts the message text out of the rendered {@code {"message": "..."}} action config. */
  private String extractMessage(String renderedConfig) {
    if (renderedConfig == null) return null;
    var matcher = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        .matcher(renderedConfig);
    return matcher.find() ? matcher.group(1) : null;
  }

  private Long extractLongId(Map<String, Object> context, String key) {
    if (context == null) return null;
    Object value = context.get(key);
    if (value instanceof Long l) return l;
    if (value instanceof Number n) return n.longValue();
    if (value instanceof String s && !s.isBlank()) {
      try {
        return Long.valueOf(s);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private void handleNotifyAssignee(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    Long taskId = extractLongId(context, "taskId");
    if (taskId == null) {
      log.info("[Automation:{}] NOTIFY_ASSIGNEE skipped — trigger context has no taskId", automation.getId());
      return;
    }
    Task task = taskRepository.findById(taskId).orElse(null);
    if (task == null) {
      log.info("[Automation:{}] NOTIFY_ASSIGNEE skipped — task {} not found", automation.getId(), taskId);
      return;
    }
    Person assignee = task.getAssignee();
    User assigneeUser = assignee != null ? assignee.getUser() : null;
    if (assigneeUser == null) {
      log.info("[Automation:{}] NOTIFY_ASSIGNEE skipped — task {} has no assignee with a linked user account",
          automation.getId(), taskId);
      return;
    }
    notificationService.notifyAutomationTriggered(assigneeUser, automation.getName(), extractMessage(config), task);
    log.info("[Automation:{}] NOTIFY_ASSIGNEE delivered to user {} for task {}", automation.getId(),
        assigneeUser.getId(), taskId);
  }

  private void handleNotifyProjectMembers(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    if (automation.getProject() == null) {
      log.info("[Automation:{}] NOTIFY_PROJECT_MEMBERS skipped — automation is not scoped to a project",
          automation.getId());
      return;
    }
    Long taskId = extractLongId(context, "taskId");
    Task task = taskId != null ? taskRepository.findById(taskId).orElse(null) : null;
    String message = extractMessage(config);
    for (UserProject member : userProjectRepository.findByProjectIdEager(automation.getProject().getId())) {
      notificationService.notifyAutomationTriggered(member.getUser(), automation.getName(), message, task);
    }
    log.info("[Automation:{}] NOTIFY_PROJECT_MEMBERS delivered to project {} members", automation.getId(),
        automation.getProject().getId());
  }

  /**
   * NOT YET IMPLEMENTED. Would need an HTTP client dispatching a POST to the URL in
   * {@code actionConfig}, plus SSRF-safe URL validation (reject internal/private IP ranges,
   * enforce a timeout, cap response size) before this can safely fire on operator-controlled
   * URLs. Currently only logs.
   */
  private void handleSendWebhook(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] SEND_WEBHOOK not implemented — config={}", automation.getId(), config);
  }

  /**
   * NOT YET IMPLEMENTED. Would route through {@code IEmailNotificationService} with a
   * subject/body extracted from {@code actionConfig} and a resolved recipient. Currently only
   * logs.
   */
  private void handleSendEmail(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] SEND_EMAIL not implemented — config={}", automation.getId(), config);
  }

  /**
   * NOT YET IMPLEMENTED. Would create a system-authored {@code Comment} on the triggering task
   * or pitch via the existing comment service/repository. Currently only logs.
   */
  private void handleAddComment(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] ADD_COMMENT not implemented — config={}", automation.getId(), config);
  }

  /**
   * NOT YET IMPLEMENTED. Would resolve the target task from context and call
   * {@code TaskService} to change its status to the value in {@code actionConfig}. Currently
   * only logs.
   */
  private void handleChangeTaskStatus(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] CHANGE_TASK_STATUS not implemented — config={}", automation.getId(), config);
  }

  /**
   * NOT YET IMPLEMENTED. Would call {@code TaskService} to create a new task in
   * {@code automation.getProject()} with title/status from {@code actionConfig}. Currently only
   * logs.
   */
  private void handleCreateTask(WorkflowAutomation automation, String config,
      Map<String, Object> context) {
    log.info("[Automation:{}] CREATE_TASK not implemented — config={}", automation.getId(), config);
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
