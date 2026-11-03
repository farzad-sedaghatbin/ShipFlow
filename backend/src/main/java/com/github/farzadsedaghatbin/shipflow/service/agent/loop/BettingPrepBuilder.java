package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.betting.CapacityAnalysisDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.BettingTableService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The brief for a betting meeting (Shape Up): the shaped pitches waiting for a bet, their
 * appetite, and how much capacity the next cycle has. The numbers come from the same betting-table
 * service the betting screen uses, so the brief and the screen cannot disagree.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BettingPrepBuilder {

  private final CycleRepository cycleRepository;
  private final BettingTableService bettingTableService;

  public AgentLoopReport build(Project project, LocalDate today) {
    String title = "Betting table prep — " + OutboundText.line(project.getName(), 80);
    if (project.getProjectType() != ProjectType.SHAPE_UP) {
      return new AgentLoopReport(
          title, "Betting prep only applies to Shape Up projects.", false, "PROJECT", project.getId());
    }

    List<PitchDTO> candidates = bettingTableService.getShapedPitchesForBetting(project.getId());
    Cycle next = cycleRepository.findByProjectIdOrderByStartDateAsc(project.getId()).stream()
        .filter(c -> c.getStartDate() != null && !c.getStartDate().isBefore(today))
        .findFirst().orElse(null);

    StringBuilder out = new StringBuilder();
    if (next != null) {
      out.append("Next cycle: ").append(OutboundText.line(next.getName(), 80)).append(" (")
          .append(next.getStartDate()).append(" → ").append(next.getEndDate()).append(")\n");
    } else {
      out.append("No upcoming cycle is planned yet.\n");
    }

    boolean warnings = false;
    if (next != null) {
      CapacityAnalysisDTO capacity = capacity(next.getId());
      if (capacity != null) {
        out.append("Capacity: ").append(capacity.getAllocatedWeeks()).append(" of ")
            .append(capacity.getTotalCapacityWeeks()).append(" team-weeks already allocated, ")
            .append(capacity.getRemainingWeeks()).append(" left\n");
        if (capacity.getWarnings() != null) {
          for (CapacityAnalysisDTO.CapacityWarning w : capacity.getWarnings()) {
            out.append("  ⚠ ").append(OutboundText.line(w.getMessage(), 140)).append('\n');
            warnings = true;
          }
        }
      }
    }
    out.append('\n');

    List<String> pitches = candidates.stream()
        .map(p -> OutboundText.line(p.getTitle(), 90)
            + (p.getAppetiteDays() != null ? " — " + p.getAppetiteDays() + " days appetite" : ""))
        .toList();
    if (pitches.isEmpty()) {
      out.append("No shaped pitches are waiting for a bet.\n");
    } else {
      ReportText.section(out, "🎯 Shaped pitches ready to bet on", pitches);
      int totalDays = candidates.stream().mapToInt(p -> p.getAppetiteDays() == null ? 0 : p.getAppetiteDays()).sum();
      out.append("Total appetite across them: ").append(totalDays).append(" days\n");
    }
    return new AgentLoopReport(
        title, out.toString().trim(), !pitches.isEmpty() || warnings, next != null ? "CYCLE" : "PROJECT",
        next != null ? next.getId() : project.getId());
  }

  private CapacityAnalysisDTO capacity(Long cycleId) {
    try {
      return bettingTableService.getCapacityAnalysis(cycleId);
    } catch (Exception e) {
      log.debug("Capacity analysis unavailable for cycle {}: {}", cycleId, e.getMessage());
      return null;
    }
  }
}
