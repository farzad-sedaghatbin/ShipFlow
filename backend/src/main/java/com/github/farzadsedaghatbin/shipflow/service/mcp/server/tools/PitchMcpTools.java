package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MCP tool implementations for pitch operations.
 *
 * <p>Pitches are the core Shape Up artefact: a shaped problem + solution with appetite (budget),
 * risk notes, and wireframe links (typically Figma URLs). The wireframe links are especially
 * valuable for AI assistants: after calling {@code get_pitch_detail}, the assistant can pass the
 * Figma URL to a Figma MCP server to retrieve the actual design context for implementation.
 *
 * <p>Example AI workflow:
 *
 * <ol>
 *   <li>Call {@code get_pitches} to find the pitch for the current sprint
 *   <li>Call {@code get_pitch_detail} to get wireframeLinks
 *   <li>Pass the Figma URL to {@code get_design_context} via Figma MCP
 *   <li>Use the combined context to generate component code
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PitchMcpTools {

  private final PitchService pitchService;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_GET_PITCHES = "get_pitches";
  public static final String TOOL_GET_PITCH = "get_pitch_detail";
  public static final String TOOL_GET_BETTING_CANDIDATES = "get_betting_candidates";

  public static Map<String, Object> getPitchesDefinition() {
    return Map.of(
        "name", TOOL_GET_PITCHES,
        "description",
            "List pitches for a cycle or project. Returns id, title, status "
                + "(IDEA, DRAFT, SHAPED, PENDING, IN_PROGRESS, COMPLETED, ABANDONED), "
                + "appetiteDays, teamName, cycleId, and whether wireframeLinks are present. "
                + "Use get_pitch_detail to get the full Shape Up fields and wireframe URLs.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "cycleId",
                        Map.of(
                            "type", "integer",
                            "description", "Filter by cycle ID"),
                        "projectId",
                        Map.of(
                            "type", "integer",
                            "description", "Filter by project ID")),
                "required", List.of()));
  }

  public static Map<String, Object> getPitchDetailDefinition() {
    return Map.of(
        "name", TOOL_GET_PITCH,
        "description",
            "Get full pitch details including Shape Up methodology fields: "
                + "problemStatement, solution, rabbitHoles, risks, noGos, and wireframeLinks. "
                + "wireframeLinks typically contains Figma URLs — pass them to Figma MCP "
                + "(get_design_context) to retrieve design context for implementation.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "pitchId",
                        Map.of("type", "integer", "description", "The numeric pitch ID")),
                "required", List.of("pitchId")));
  }

  public static Map<String, Object> getBettingCandidatesDefinition() {
    return Map.of(
        "name", TOOL_GET_BETTING_CANDIDATES,
        "description",
            "List shaped pitches that are candidates for the next betting table — "
                + "i.e., pitches with status SHAPED that have not yet been assigned to a cycle. "
                + "Includes wireframeLinks so AI can fetch Figma designs during planning.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId",
                        Map.of(
                            "type", "integer",
                            "description", "Optional: filter by project ID")),
                "required", List.of()));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public List<McpPitchDTO> getPitches(Map<String, Object> args) {
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");

    if (cycleIdArg != null) {
      return pitchService.getPitchesByCycleId(toLong(cycleIdArg)).stream()
          .map(McpPitchDTO::from)
          .toList();
    }
    if (projectIdArg != null) {
      // Use accessible pitches filtered client-side by project
      return pitchService.getAccessiblePitches().stream()
          .filter(p -> toLong(projectIdArg) == p.getProjectId())
          .map(McpPitchDTO::from)
          .toList();
    }
    return pitchService.getAccessiblePitches().stream()
        .map(McpPitchDTO::from)
        .toList();
  }

  public McpPitchDTO getPitchDetail(Map<String, Object> args) {
    long pitchId = toLong(args.get("pitchId"));
    return McpPitchDTO.from(pitchService.getPitchById(pitchId));
  }

  public List<McpPitchDTO> getBettingCandidates(Map<String, Object> args) {
    Object projectIdArg = args.get("projectId");
    if (projectIdArg != null) {
      return pitchService.getBettingCandidatesByProjectId(toLong(projectIdArg)).stream()
          .map(McpPitchDTO::from)
          .toList();
    }
    return pitchService.getBettingCandidates().stream()
        .map(McpPitchDTO::from)
        .toList();
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
