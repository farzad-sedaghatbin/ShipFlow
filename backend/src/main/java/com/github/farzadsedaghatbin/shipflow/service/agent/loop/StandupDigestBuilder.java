package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The daily standup digest: what moved in a project over the look-back window, and what is stuck.
 * Works for every methodology because it only reads tasks and bugs. Every line comes from the
 * database; nothing here is written by a model.
 */
@Component
@RequiredArgsConstructor
public class StandupDigestBuilder {

  private final TaskRepository taskRepository;
  private final BugReportRepository bugReportRepository;

  public AgentLoopReport build(Project project, AgentLoopConfig config, LocalDateTime now) {
    Long pid = project.getId();
    LocalDateTime since = now.minusHours(config.lookbackHours());

    List<String> finished = taskRepository.findCompletedSince(pid, TaskStatus.DONE, since).stream()
        .map(t -> task(t)).toList();
    List<String> inProgress = taskRepository
        .findByProjectAndStatusIn(pid, List.of(TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW)).stream()
        .map(t -> task(t) + (t.getStatus() == TaskStatus.IN_REVIEW ? " (in review)" : "")).toList();
    List<String> blocked = taskRepository.findByProjectAndStatusIn(pid, List.of(TaskStatus.BLOCKED)).stream()
        .map(t -> task(t)).toList();
    List<String> overdue = taskRepository
        .findOverdueByProject(pid, now.toLocalDate(), List.of(TaskStatus.DONE, TaskStatus.CANCELLED)).stream()
        .map(t -> task(t) + " — due " + t.getDueDate()).toList();
    List<String> bugs = bugReportRepository.findCreatedSince(pid, since).stream().map(StandupDigestBuilder::bug).toList();

    boolean any = !(finished.isEmpty() && inProgress.isEmpty() && blocked.isEmpty() && overdue.isEmpty() && bugs.isEmpty());

    StringBuilder out = new StringBuilder();
    out.append("Last ").append(config.lookbackHours()).append(" hours in ")
        .append(OutboundText.line(project.getName(), 80)).append("\n\n");
    if (!any) {
      out.append("Nothing moved and nothing is stuck.\n");
    }
    ReportText.section(out, "✅ Finished", finished);
    ReportText.section(out, "🔄 In progress", inProgress);
    ReportText.section(out, "⛔ Blocked", blocked);
    ReportText.section(out, "⏰ Overdue", overdue);
    ReportText.section(out, "🐞 New bugs", bugs);

    return new AgentLoopReport(
        "Standup digest — " + OutboundText.line(project.getName(), 80),
        out.toString().trim(), any, "PROJECT", pid);
  }

  private static String task(Task t) {
    return OutboundText.line(t.getTitle(), 90) + " — " + ReportText.who(t.getAssignee());
  }

  private static String bug(BugReport b) {
    String key = b.getBugKey() != null ? b.getBugKey() + " " : "";
    String sev = b.getSeverity() != null ? " [" + b.getSeverity() + "]" : "";
    return key + OutboundText.line(b.getTitle(), 90) + sev;
  }
}
