package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpCycleDTO;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MCP tool implementations for cycle operations.
 *
 * <p>Cycles are the core Shape Up delivery unit: 6-week periods during which shaped pitches are
 * built. Kanban projects use a continuous "default" cycle.
 */
@Component
@RequiredArgsConstructor
public class CycleMcpTools {

  private final CycleService cycleService;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_GET_CYCLES = "get_cycles";
  public static final String TOOL_GET_CYCLE = "get_cycle";

  public static Map<String, Object> getCyclesDefinition() {
    return Map.of(
        "name", TOOL_GET_CYCLES,
        "description",
            "List cycles for a project. Without a projectId, returns all accessible cycles. "
                + "Returns id, name, phase (PLANNING, BUILDING, COOL_DOWN, COMPLETED), "
                + "startDate, endDate, isActive, and pitchCount.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId",
                        Map.of(
                            "type", "integer",
                            "description", "Optional: filter cycles by project ID")),
                "required", List.of()));
  }

  public static Map<String, Object> getCycleDefinition() {
    return Map.of(
        "name", TOOL_GET_CYCLE,
        "description",
            "Get a single cycle by ID. Returns full cycle details including phase, dates, "
                + "pitch count, and project context.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "cycleId",
                        Map.of("type", "integer", "description", "The numeric cycle ID")),
                "required", List.of("cycleId")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public List<McpCycleDTO> getCycles(Map<String, Object> args) {
    Object projectIdArg = args.get("projectId");
    if (projectIdArg != null) {
      long projectId = toLong(projectIdArg);
      return cycleService.getCyclesByProject(projectId).stream()
          .map(McpCycleDTO::from)
          .toList();
    }
    return cycleService.getAccessibleCycles().stream()
        .map(McpCycleDTO::from)
        .toList();
  }

  public McpCycleDTO getCycle(Map<String, Object> args) {
    long cycleId = toLong(args.get("cycleId"));
    return McpCycleDTO.from(cycleService.getCycleById(cycleId));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private long toLong(Object val) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument");
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Argument must be a number, got: " + val);
    }
  }
}
