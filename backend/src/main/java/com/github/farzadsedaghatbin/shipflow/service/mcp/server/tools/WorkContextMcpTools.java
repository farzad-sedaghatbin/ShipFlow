package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.HillChartPointDTO;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpCycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpWorkContextDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpWorkContextDTO.McpHillChartScopeDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpWorkContextDTO.McpRetroSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import com.github.farzadsedaghatbin.shipflow.service.HillChartService;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import com.github.farzadsedaghatbin.shipflow.service.RetroService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MCP tool that returns the full relationship graph for a pitch or cycle in a single call.
 *
 * <p>Analogous to Atlassian's Teamwork Graph: given one entry point (pitch or cycle), the tool
 * traverses all connected entities — cycle metadata, pitch details, tasks with status breakdown,
 * blockers, hill-chart scopes, and retrospective summaries — so an AI agent has complete context
 * without multiple round trips.
 *
 * <p>Typical workflow:
 * <ol>
 *   <li>Call {@code list_projects} to pick a project
 *   <li>Call {@code get_cycles} to find the active cycle
 *   <li>Call {@code get_work_context} with the cycleId or a specific pitchId
 *   <li>The agent now has the full graph — it can identify blockers, check hill-chart progress,
 *       read the retro notes, and understand the entire state of work in one shot
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WorkContextMcpTools {

  private final PitchService pitchService;
  private final CycleService cycleService;
  private final TaskService taskService;
  private final HillChartService hillChartService;
  private final RetroService retroService;

  public static final String TOOL_GET_WORK_CONTEXT = "get_work_context";

  public static Map<String, Object> getWorkContextDefinition() {
    return Map.of(
        "name", TOOL_GET_WORK_CONTEXT,
        "description",
            "Return the full relationship graph for a pitch or cycle in a single call. "
                + "Includes: cycle metadata, pitch details (problem/solution/risks/wireframes), "
                + "all tasks with status breakdown and blocked count, "
                + "hill-chart scope positions (0=start, 50=top of hill, 100=shipped), "
                + "and retrospective summaries. "
                + "Use this instead of calling get_cycle + get_pitch_detail + get_tasks + get_blockers separately. "
                + "Provide either pitchId (graph scoped to that pitch) or cycleId (graph for the whole cycle).",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "pitchId",
                        Map.of(
                            "type", "integer",
                            "description",
                                "ID of a specific pitch — returns context scoped to that pitch "
                                    + "and its parent cycle"),
                        "cycleId",
                        Map.of(
                            "type", "integer",
                            "description",
                                "ID of a cycle — returns context for all pitches and tasks in "
                                    + "the cycle. pitchId takes precedence if both are provided.")),
                "required", List.of()));
  }

  // ── Implementation ────────────────────────────────────────────────────────

  public McpWorkContextDTO getWorkContext(Map<String, Object> args) {
    Object pitchIdArg = args.get("pitchId");
    Object cycleIdArg = args.get("cycleId");

    if (pitchIdArg == null && cycleIdArg == null) {
      throw new IllegalArgumentException(
          "Either 'pitchId' or 'cycleId' must be provided.");
    }

    if (pitchIdArg != null) {
      return buildContextForPitch(toLong(pitchIdArg, "pitchId"));
    }
    return buildContextForCycle(toLong(cycleIdArg, "cycleId"));
  }

  // ── Graph builders ────────────────────────────────────────────────────────

  private McpWorkContextDTO buildContextForPitch(long pitchId) {
    PitchDTO pitch = pitchService.getPitchById(pitchId);
    McpPitchDTO mcpPitch = McpPitchDTO.from(pitch);

    Long cycleId = pitch.getCycleId();
    McpCycleDTO mcpCycle = cycleId != null
        ? McpCycleDTO.from(cycleService.getCycleById(cycleId))
        : null;

    List<TaskDTO> rawTasks = taskService.getTasksByPitchId(pitchId);
    List<McpTaskDTO> tasks = rawTasks.stream().map(McpTaskDTO::from).toList();
    List<McpTaskDTO> blockers = tasks.stream()
        .filter(t -> Boolean.TRUE.equals(t.getIsBlocked()))
        .toList();

    List<McpHillChartScopeDTO> hillChart = hillChartService
        .getHillChartPointsByPitch(pitchId).stream()
        .map(WorkContextMcpTools::toHillScopeDTO)
        .toList();

    List<McpRetroSummaryDTO> retros = cycleId != null
        ? retroService.getRetrosByCycle(cycleId).stream()
            .map(WorkContextMcpTools::toRetroSummaryDTO)
            .toList()
        : List.of();

    return McpWorkContextDTO.builder()
        .cycle(mcpCycle)
        .pitch(mcpPitch)
        .pitches(List.of(mcpPitch))
        .tasks(tasks)
        .taskStatusCounts(countByStatus(tasks))
        .blockers(blockers)
        .hillChartScopes(hillChart)
        .retrospectives(retros)
        .build();
  }

  private McpWorkContextDTO buildContextForCycle(long cycleId) {
    McpCycleDTO mcpCycle = McpCycleDTO.from(cycleService.getCycleById(cycleId));

    List<McpPitchDTO> pitches = pitchService.getPitchesByCycleId(cycleId).stream()
        .map(McpPitchDTO::from)
        .toList();

    List<TaskDTO> rawTasks = taskService.getTasksByCycleId(cycleId);
    List<McpTaskDTO> tasks = rawTasks.stream().map(McpTaskDTO::from).toList();
    List<McpTaskDTO> blockers = tasks.stream()
        .filter(t -> Boolean.TRUE.equals(t.getIsBlocked()))
        .toList();

    List<McpHillChartScopeDTO> hillChart = hillChartService
        .getHillChartPointsByCycle(cycleId).stream()
        .map(WorkContextMcpTools::toHillScopeDTO)
        .toList();

    List<McpRetroSummaryDTO> retros = retroService.getRetrosByCycle(cycleId).stream()
        .map(WorkContextMcpTools::toRetroSummaryDTO)
        .toList();

    return McpWorkContextDTO.builder()
        .cycle(mcpCycle)
        .pitch(null)
        .pitches(pitches)
        .tasks(tasks)
        .taskStatusCounts(countByStatus(tasks))
        .blockers(blockers)
        .hillChartScopes(hillChart)
        .retrospectives(retros)
        .build();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static Map<String, Integer> countByStatus(List<McpTaskDTO> tasks) {
    Map<String, Integer> counts = new HashMap<>();
    for (McpTaskDTO task : tasks) {
      String status = task.getStatus() != null ? task.getStatus() : "UNKNOWN";
      counts.merge(status, 1, Integer::sum);
    }
    return counts;
  }

  private static McpHillChartScopeDTO toHillScopeDTO(HillChartPointDTO p) {
    return McpHillChartScopeDTO.builder()
        .id(p.getId())
        .scope(p.getScope())
        .description(p.getDescription())
        .position(p.getPosition())
        .pitchId(p.getPitchId())
        .pitchTitle(p.getPitchTitle())
        .linkedTaskId(p.getLinkedTaskId())
        .linkedTaskTitle(p.getLinkedTaskTitle())
        .build();
  }

  private static McpRetroSummaryDTO toRetroSummaryDTO(RetroDTO r) {
    return McpRetroSummaryDTO.builder()
        .id(r.getId())
        .title(r.getTitle())
        .status(r.getStatus() != null ? r.getStatus().name() : null)
        .itemCount(r.getItemCount())
        .cycleId(r.getCycleId())
        .cycleName(r.getCycleName())
        .build();
  }

  private long toLong(Object val, String argName) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Argument '" + argName + "' must be a number, got: " + val);
    }
  }
}
