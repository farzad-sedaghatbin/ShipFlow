package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.service.email.EmailAddressResolver;
import com.github.farzadsedaghatbin.shipflow.service.email.EmailRecipientService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dispatches the configured action for a matched workflow automation rule.
 *
 * <p>NOTIFY_ASSIGNEE and NOTIFY_PROJECT_MEMBERS deliver in-app (and, when SMTP is configured, by email)
 * via {@link DashboardNotificationService}; SEND_EMAIL emails directly. The other four action
 * types (SEND_WEBHOOK, ADD_COMMENT, CHANGE_TASK_STATUS, CREATE_TASK) are recognized
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
  private final AgentLoopService agentLoopService;
  private final IEmailNotificationService emailService;
  private final EmailRecipientService emailRecipients;

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int MAX_EMAIL_RECIPIENTS = 25;

  /**
   * Runs the automation's action.
   *
   * @return what happened, in words, for the execution log; {@code null} when the action has
   *     nothing more to say than "it ran"
   */
  public String dispatch(WorkflowAutomation automation, Map<String, Object> context) {
    ActionType action = automation.getActionType();
    String config = automation.getActionConfig() != null ? automation.getActionConfig() : "{}";
    String rendered = renderTemplate(config, context);

    String detail = null;
    switch (action) {
      case AGENT_STANDUP_DIGEST, AGENT_CYCLE_HEALTH_CHECK, AGENT_BETTING_PREP ->
          detail = agentLoopService.run(automation, context);
      case NOTIFY_ASSIGNEE -> handleNotifyAssignee(automation, rendered, context);
      case NOTIFY_PROJECT_MEMBERS -> handleNotifyProjectMembers(automation, rendered, context);
      case SEND_WEBHOOK -> handleSendWebhook(automation, rendered, context);
      case SEND_EMAIL -> detail = handleSendEmail(automation, config, context);
      case ADD_COMMENT -> handleAddComment(automation, rendered, context);
      case CHANGE_TASK_STATUS -> handleChangeTaskStatus(automation, rendered, context);
      case CREATE_TASK -> handleCreateTask(automation, rendered, context);
      default -> log.warn("Unknown action type: {}", action);
    }
    return detail;
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
   * Emails the people named in the rule. Config: {@code {"subject": "...", "body": "...", "to": ...}}
   * where {@code to} is optional and is a keyword or an address, or an array mixing them:
   * {@code assignee} (the triggering task's assignee), {@code project_members}, {@code project_managers}
   * (members with the project MANAGER role), or an email address. Without {@code to} the rule emails
   * the project's members. {@code {{placeholders}}} in subject and body are filled from the trigger.
   *
   * <p>Fails (so the execution log shows a FAILURE) rather than pretending: when SMTP is not
   * configured, or when nobody could be emailed.
   */
  private String handleSendEmail(WorkflowAutomation automation, String rawConfig, Map<String, Object> context) {
    if (!emailService.isEnabled()) {
      throw new IllegalStateException("Email is not configured on this server (SMTP_HOST is not set)");
    }
    JsonNode config;
    try {
      config = MAPPER.readTree(rawConfig == null || rawConfig.isBlank() ? "{}" : rawConfig);
    } catch (Exception e) {
      throw new IllegalStateException("The email action's settings are not valid JSON");
    }
    String subject = fill(config.path("subject").asText(""), context);
    if (subject.isBlank()) {
      subject = automation.getName() != null ? automation.getName() : "ShipFlow automation";
    }
    String body = fill(config.path("body").asText(config.path("message").asText("")), context);

    Long taskId = extractLongId(context, "taskId");
    Task task = taskId != null ? taskRepository.findById(taskId).orElse(null) : null;
    String actionUrl = actionUrl(context, taskId);

    // address (lower-case) -> display name; insertion order kept, duplicates collapse
    Map<String, String[]> recipients = new LinkedHashMap<>();
    JsonNode to = config.path("to");
    java.util.List<String> targets = new java.util.ArrayList<>();
    if (to.isArray()) {
      to.forEach(n -> targets.add(n.asText("")));
    } else if (to.isTextual() && !to.asText().isBlank()) {
      for (String part : to.asText().split("[,;]")) {
        targets.add(part);
      }
    }
    if (targets.isEmpty()) {
      targets.add("project_members");
    }
    for (String raw : targets) {
      String target = raw.trim();
      String key = target.toLowerCase(Locale.ROOT);
      switch (key) {
        case "" -> {
        }
        case "assignee" -> {
          Person assignee = task != null ? task.getAssignee() : null;
          addUser(recipients, assignee != null ? assignee.getUser() : null);
        }
        case "project_members", "project_managers" -> {
          if (automation.getProject() != null) {
            for (UserProject member : userProjectRepository.findByProjectIdEager(automation.getProject().getId())) {
              if (key.equals("project_members") || member.getProjectRole() == ProjectRole.MANAGER) {
                addUser(recipients, member.getUser());
              }
            }
          }
        }
        default -> {
          String address = EmailAddressResolver.usable(target);
          if (address != null) {
            recipients.putIfAbsent(address.toLowerCase(Locale.ROOT), new String[] {address, address});
          }
        }
      }
    }
    if (recipients.isEmpty()) {
      throw new IllegalStateException(
          "Nobody to email: no matching recipient has a usable email address (check the rule's \"to\" setting)");
    }
    int sent = 0;
    for (String[] r : recipients.values()) {
      if (sent >= MAX_EMAIL_RECIPIENTS) {
        break;
      }
      emailService.sendAutomationEmail(r[0], r[1], subject, body, actionUrl);
      sent++;
    }
    log.info("[Automation:{}] SEND_EMAIL queued for {} recipient(s)", automation.getId(), sent);
    return "Emailed " + sent + (sent == 1 ? " recipient" : " recipients");
  }

  /** Adds the user if they have an emailable address (resolved by id, outside any lazy-loading trap). */
  private void addUser(Map<String, String[]> recipients, User user) {
    if (user == null) {
      return;
    }
    emailRecipients.forUserId(user.getId())
        .ifPresent(r -> recipients.putIfAbsent(r.address().toLowerCase(Locale.ROOT), new String[] {r.address(), r.name()}));
  }

  private String actionUrl(Map<String, Object> context, Long taskId) {
    if (taskId != null) {
      return "/backlog/" + taskId;
    }
    Long pitchId = extractLongId(context, "pitchId");
    if (pitchId != null) {
      return "/pitches/" + pitchId;
    }
    Long cycleId = extractLongId(context, "cycleId");
    return cycleId != null ? "/cycles/" + cycleId : null;
  }

  /** Fills {{placeholders}} in plain text (not JSON), so a value containing quotes cannot break anything. */
  private String fill(String text, Map<String, Object> context) {
    return renderTemplate(text, context);
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
