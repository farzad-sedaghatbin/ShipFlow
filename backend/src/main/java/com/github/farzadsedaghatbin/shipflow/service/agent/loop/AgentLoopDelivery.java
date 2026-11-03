package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.DashboardNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopConfig.Channel;
import com.github.farzadsedaghatbin.shipflow.service.notification.NotificationProvider;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends a finished report to the channels the automation asked for.
 *
 * <p>Slack and Teams swallow their own delivery errors (they log and record history rather than
 * throw) and report nothing back, so "sent" here means "handed to the provider that is configured
 * and active" — the integration's notification history is where an actual failure shows up. A
 * channel that is requested but not configured is reported as skipped, so a misconfigured digest
 * is visible in the execution log instead of looking like it worked.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentLoopDelivery {

  /** What happened per channel, in words for the execution log. */
  public record Outcome(List<String> sent, List<String> skipped) {
    public boolean anySent() {
      return !sent.isEmpty();
    }

    public String describe() {
      StringBuilder sb = new StringBuilder();
      sb.append(sent.isEmpty() ? "Nothing was sent" : "Sent to " + String.join(", ", sent));
      if (!skipped.isEmpty()) {
        sb.append("; skipped: ").append(String.join(", ", skipped));
      }
      return sb.toString();
    }
  }

  /** In-app notifications are short; the full report is in the channel messages and the preview. */
  static final int IN_APP_MAX = 480;

  private final List<NotificationProvider> providers;
  private final UserProjectRepository userProjectRepository;
  private final DashboardNotificationService notificationService;

  public Outcome deliver(
      Project project, String automationName, String notificationType, AgentLoopReport report,
      String summary, AgentLoopConfig config) {
    List<String> sent = new ArrayList<>();
    List<String> skipped = new ArrayList<>();
    String message = compose(report, summary);

    for (Channel channel : config.channels()) {
      switch (channel) {
        case SLACK -> external("slack", "Slack", notificationType, message, config, report, sent, skipped);
        case TEAMS -> external("teams", "Teams", notificationType, message, config, report, sent, skipped);
        case IN_APP -> inApp(project, automationName, report, summary, sent, skipped);
      }
    }
    return new Outcome(sent, skipped);
  }

  /** The text a channel receives: heading, the agent's summary (if any), then the facts. */
  public static String compose(AgentLoopReport report, String summary) {
    StringBuilder sb = new StringBuilder("*").append(report.title()).append("*\n");
    if (summary != null && !summary.isBlank()) {
      sb.append(summary).append("\n\n");
    } else {
      sb.append('\n');
    }
    sb.append(report.facts()).append("\n\n— ShipFlow Agent");
    return sb.toString();
  }

  private void external(
      String providerName, String label, String type, String message, AgentLoopConfig config,
      AgentLoopReport report, List<String> sent, List<String> skipped) {
    NotificationProvider provider = providers.stream()
        .filter(p -> providerName.equals(p.getProviderName())).findFirst().orElse(null);
    if (provider == null || !safeActive(provider)) {
      skipped.add(label + " (not configured)");
      return;
    }
    try {
      provider.sendNotification(type, message, config.channel(), report.entityType(), report.entityId());
      sent.add(label);
    } catch (Exception e) {
      log.warn("Agent loop could not hand its report to {}: {}", label, e.getMessage());
      skipped.add(label + " (failed: " + OutboundText.line(e.getMessage(), 80) + ")");
    }
  }

  private static boolean safeActive(NotificationProvider provider) {
    try {
      return provider.isActive();
    } catch (Exception e) {
      return false;
    }
  }

  private void inApp(
      Project project, String automationName, AgentLoopReport report, String summary,
      List<String> sent, List<String> skipped) {
    List<UserProject> members = userProjectRepository.findByProjectIdEager(project.getId());
    if (members.isEmpty()) {
      skipped.add("In-app (the project has no members)");
      return;
    }
    String body = (summary != null && !summary.isBlank() ? summary + "\n\n" : "") + report.facts();
    if (body.length() > IN_APP_MAX) {
      body = body.substring(0, IN_APP_MAX - 1) + "…";
    }
    for (UserProject member : members) {
      notificationService.notifyAutomationTriggered(member.getUser(), automationName, body, null);
    }
    sent.add("In-app (" + members.size() + " member" + (members.size() == 1 ? "" : "s") + ")");
  }
}
