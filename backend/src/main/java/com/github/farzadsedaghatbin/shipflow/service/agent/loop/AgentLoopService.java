package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The ShipFlow Agent's scheduled loops (S71): standup digest, cycle-health check and betting-table
 * prep. Each run gathers facts from the database ({@link StandupDigestBuilder} and friends), lets
 * the model add at most a short checked summary ({@link AgentLoopNarrator}), and hands the result
 * to the channels the automation chose ({@link AgentLoopDelivery}).
 *
 * <p>Runs as the system, not as a person: it reads one project's data and posts into channels the
 * automation's manager chose, never acts on anything, and only for the project the automation
 * belongs to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentLoopService {

  /** The action types this service runs. */
  public static final Set<ActionType> LOOP_ACTIONS = Set.of(
      ActionType.AGENT_STANDUP_DIGEST, ActionType.AGENT_CYCLE_HEALTH_CHECK, ActionType.AGENT_BETTING_PREP);

  private final ObjectMapper objectMapper;
  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final StandupDigestBuilder standupBuilder;
  private final CycleHealthBuilder healthBuilder;
  private final BettingPrepBuilder bettingBuilder;
  private final AgentLoopNarrator narrator;
  private final AgentLoopDelivery delivery;

  public static boolean isLoopAction(ActionType type) {
    return type != null && LOOP_ACTIONS.contains(type);
  }

  /** A generated report that has not been sent anywhere. */
  public record Preview(String title, String summary, String facts, String message, boolean wouldSend) {}

  /**
   * Builds and delivers the loop. Returns what happened, in words, for the execution log.
   *
   * @param context the trigger context; for event-triggered betting prep it carries the cycle that
   *     just ended, which must belong to this automation's project
   */
  public String run(WorkflowAutomation automation, Map<String, Object> context) {
    AgentLoopConfig config = AgentLoopConfig.parse(automation.getActionConfig(), objectMapper);
    Project project = projectOf(automation);

    String foreign = foreignCycleReason(automation, project, context);
    if (foreign != null) {
      return "Skipped: " + foreign;
    }

    AgentLoopReport report = build(automation.getActionType(), project, config);
    if (!shouldSend(automation.getActionType(), report, config)) {
      return "Skipped: " + quietReason(automation.getActionType());
    }
    String summary = config.useAi() ? narrator.summarize(report) : null;
    AgentLoopDelivery.Outcome outcome = delivery.deliver(
        project, automation.getName(), notificationType(automation.getActionType()), report, summary, config);
    log.info("[Automation:{}] {} — {}", automation.getId(), automation.getActionType(), outcome.describe());
    if (!outcome.anySent()) {
      // Nothing reached anyone: that is a failed run, not a successful one.
      throw new IllegalStateException(outcome.describe());
    }
    return outcome.describe() + (summary != null ? " (with an AI summary)" : "");
  }

  /** What the loop would send right now, without sending it. */
  public Preview preview(WorkflowAutomation automation) {
    AgentLoopConfig config = AgentLoopConfig.parse(automation.getActionConfig(), objectMapper);
    Project project = projectOf(automation);
    AgentLoopReport report = build(automation.getActionType(), project, config);
    boolean send = shouldSend(automation.getActionType(), report, config);
    String summary = config.useAi() ? narrator.summarize(report) : null;
    return new Preview(
        report.title(), summary, report.facts(), AgentLoopDelivery.compose(report, summary), send);
  }

  // ── internals ────────────────────────────────────────────────────────────────

  AgentLoopReport build(ActionType type, Project project, AgentLoopConfig config) {
    return switch (type) {
      case AGENT_STANDUP_DIGEST -> standupBuilder.build(project, config, LocalDateTime.now());
      case AGENT_CYCLE_HEALTH_CHECK -> healthBuilder.build(project, config, LocalDate.now());
      case AGENT_BETTING_PREP -> bettingBuilder.build(project, LocalDate.now());
      default -> throw new IllegalArgumentException("Not an agent loop: " + type);
    };
  }

  /** A loop with nothing to say stays quiet rather than posting "nothing happened". */
  static boolean shouldSend(ActionType type, AgentLoopReport report, AgentLoopConfig config) {
    if (type == ActionType.AGENT_CYCLE_HEALTH_CHECK) {
      return report.hasFindings() || !config.onlyWhenAtRisk();
    }
    return report.hasFindings();
  }

  private static String quietReason(ActionType type) {
    return switch (type) {
      case AGENT_STANDUP_DIGEST -> "nothing to report";
      case AGENT_CYCLE_HEALTH_CHECK -> "no risks found";
      default -> "nothing to prepare";
    };
  }

  private static String notificationType(ActionType type) {
    return type.name();
  }

  private Project projectOf(WorkflowAutomation automation) {
    if (automation.getProject() == null) {
      throw new IllegalStateException("The automation is not attached to a project");
    }
    return projectRepository.findById(automation.getProject().getId())
        .orElseThrow(() -> new IllegalStateException("The automation's project no longer exists"));
  }

  /**
   * Event triggers (e.g. CYCLE_ENDED) are currently fired for every project's automations; a loop
   * must only react to its own project's cycle.
   */
  private String foreignCycleReason(WorkflowAutomation automation, Project project, Map<String, Object> context) {
    if (context == null || !(context.get("cycleId") instanceof Number n)) {
      return null;
    }
    Cycle cycle = cycleRepository.findById(n.longValue()).orElse(null);
    if (cycle != null && cycle.getProject() != null && !cycle.getProject().getId().equals(project.getId())) {
      return "the event belongs to another project";
    }
    return null;
  }
}
