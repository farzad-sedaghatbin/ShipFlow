package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.PitchHealthService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Checks the project's active cycle/sprint against the calendar. It never judges by feel: a cycle
 * is "behind" only when the share of tasks done trails the share of time elapsed by more than
 * {@link #TOLERANCE_POINTS}, and the other findings are plain counts (blocked, overdue) and — for
 * Shape Up — the pitches the existing health service already rates high-risk.
 */
@Component
@Slf4j
public class CycleHealthBuilder {

  /** How far completion may trail elapsed time before the cycle is called behind. */
  static final int TOLERANCE_POINTS = 15;

  /** Cycles reported per run (a project normally has one active). */
  private static final int MAX_CYCLES = 3;

  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;
  private final ObjectProvider<PitchHealthService> pitchHealth;

  public CycleHealthBuilder(
      CycleRepository cycleRepository, TaskRepository taskRepository, ObjectProvider<PitchHealthService> pitchHealth) {
    this.cycleRepository = cycleRepository;
    this.taskRepository = taskRepository;
    this.pitchHealth = pitchHealth;
  }

  public AgentLoopReport build(Project project, AgentLoopConfig config, LocalDate today) {
    List<Cycle> cycles = cycleRepository.findByProjectIdAndIsActiveTrue(project.getId());
    String title = "Cycle health — " + OutboundText.line(project.getName(), 80);
    if (cycles.isEmpty()) {
      return new AgentLoopReport(title, "There is no active cycle or sprint to check.", false, "PROJECT", project.getId());
    }

    StringBuilder out = new StringBuilder();
    boolean atRisk = false;
    Long firstCycleId = null;
    for (Cycle cycle : cycles.stream().limit(MAX_CYCLES).toList()) {
      firstCycleId = firstCycleId == null ? cycle.getId() : firstCycleId;
      atRisk |= describe(out, project, cycle, today);
    }
    if (atRisk) {
      out.insert(0, "⚠️ At risk\n\n");
    } else {
      out.insert(0, "✅ On track\n\n");
    }
    return new AgentLoopReport(
        title, out.toString().trim(), atRisk, cycles.size() == 1 ? "CYCLE" : "PROJECT",
        cycles.size() == 1 ? firstCycleId : project.getId());
  }

  /** Appends one cycle's section; returns true when it has at least one risk finding. */
  private boolean describe(StringBuilder out, Project project, Cycle cycle, LocalDate today) {
    List<Task> tasks = taskRepository.findByCycleIdWithAssignee(cycle.getId()).stream()
        .filter(t -> t.getStatus() != TaskStatus.CANCELLED).toList();
    long total = tasks.size();
    long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
    int donePct = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);

    out.append(OutboundText.line(cycle.getName(), 80));
    LocalDate start = cycle.getStartDate();
    LocalDate end = cycle.getEndDate();
    boolean dated = start != null && end != null && end.isAfter(start);
    int elapsedPct = -1;
    if (dated) {
      long span = ChronoUnit.DAYS.between(start, end);
      long gone = ChronoUnit.DAYS.between(start, today);
      elapsedPct = (int) Math.max(0, Math.min(100, Math.round(gone * 100.0 / span)));
      out.append(" (").append(start).append(" → ").append(end).append(")");
    }
    out.append('\n');
    out.append("  Progress: ").append(done).append(" of ").append(total).append(" tasks done (").append(donePct).append("%)");
    if (dated) {
      out.append(", ").append(elapsedPct).append("% of the time elapsed");
    }
    out.append('\n');

    List<String> findings = new ArrayList<>();
    if (dated && total > 0 && today.isAfter(start) && donePct + TOLERANCE_POINTS < elapsedPct) {
      findings.add("Behind schedule: " + donePct + "% done vs " + elapsedPct + "% of the time gone");
    }
    List<Task> blocked = tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).toList();
    if (!blocked.isEmpty()) {
      findings.add(blocked.size() + " blocked: " + names(blocked));
    }
    List<Task> overdue = tasks.stream()
        .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.DONE).toList();
    if (!overdue.isEmpty()) {
      findings.add(overdue.size() + " overdue: " + names(overdue));
    }
    if (dated && today.isAfter(end) && done < total) {
      findings.add("The cycle ended " + end + " with " + (total - done) + " task(s) still open");
    }
    findings.addAll(pitchFindings(project, cycle));

    if (findings.isEmpty()) {
      out.append("  No risks found.\n\n");
    } else {
      findings.forEach(f -> out.append("  ⚠ ").append(f).append('\n'));
      out.append('\n');
    }
    return !findings.isEmpty();
  }

  /** Shape Up only: the pitches the health service already rates high or critical risk. */
  private List<String> pitchFindings(Project project, Cycle cycle) {
    if (project.getProjectType() != ProjectType.SHAPE_UP) {
      return List.of();
    }
    PitchHealthService service = pitchHealth.getIfAvailable();
    if (service == null) {
      return List.of();
    }
    try {
      CycleHealthSummaryDTO summary = service.getCycleHealthSummary(cycle.getId(), false);
      if (summary == null || summary.getPitchHealthList() == null) {
        return List.of();
      }
      List<String> risky = summary.getPitchHealthList().stream()
          .filter(p -> p.getRiskLevel() != null
              && ("HIGH".equals(p.getRiskLevel().name()) || "CRITICAL".equals(p.getRiskLevel().name())))
          .map((PitchHealthDTO p) -> OutboundText.line(p.getPitchName(), 70) + " (" + p.getRiskLevel() + ")")
          .toList();
      return risky.isEmpty() ? List.of() : List.of(risky.size() + " high-risk pitch(es): " + String.join(", ", risky));
    } catch (Exception e) {
      log.debug("Pitch health unavailable for cycle {}: {}", cycle.getId(), e.getMessage());
      return List.of();
    }
  }

  private static String names(List<Task> tasks) {
    List<String> titles = tasks.stream().limit(3).map(t -> OutboundText.line(t.getTitle(), 50)).toList();
    return String.join(", ", titles) + (tasks.size() > 3 ? ", …" : "");
  }
}
