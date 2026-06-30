package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PitchMcpTools {

  private final PitchService pitchService;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_GET_PITCHES = "get_pitches";
  public static final String TOOL_GET_PITCH = "get_pitch_detail";
  public static final String TOOL_GET_BETTING_CANDIDATES = "get_betting_candidates";
  public static final String TOOL_CREATE_PITCH = "create_pitch";
  public static final String TOOL_UPDATE_PITCH_STATUS = "update_pitch_status";
  public static final String TOOL_UPDATE_PITCH = "update_pitch";

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

  public static Map<String, Object> createPitchDefinition() {
    return Map.of(
        "name",
        TOOL_CREATE_PITCH,
        "description",
            "Create a new pitch (Shape Up artefact). The pitch starts in IDEA status. "
                + "Supply epicId to place the pitch in the project hierarchy (Epic → Pitch). "
                + "Supply appetiteDays upfront if known — required before the pitch can be SHAPED "
                + "and committed to a betting table. "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "title",
                        Map.of("type", "string", "description", "Pitch title (required)"),
                        "problemStatement",
                        Map.of("type", "string", "description", "What problem does this solve?"),
                        "appetiteDays",
                        Map.of(
                            "type",
                            "integer",
                            "description",
                            "Time budget in days (e.g. 14 for a Small Batch, 42 for a Big Batch). "
                                + "Required before the pitch can be marked SHAPED."),
                        "epicId",
                        Map.of(
                            "type",
                            "integer",
                            "description",
                            "Optional epic ID to attach this pitch to. Places the pitch in the "
                                + "Initiative → Epic → Pitch hierarchy.")),
                "required",
                List.of("title")));
  }

  public static Map<String, Object> updatePitchStatusDefinition() {
    return Map.of(
        "name",
        TOOL_UPDATE_PITCH_STATUS,
        "description",
            "Update the status of a pitch. Pre-cycle statuses: IDEA (raw idea), DRAFT (being shaped), "
                + "SHAPED (ready for betting table), PENDING (assigned to cycle, not started). "
                + "In-cycle statuses: STARTED, IN_PROGRESS, TESTING, DONE, COOLDOWN, CANCELLED, CIRCUIT_BREAKER. "
                + "Use SHAPED to mark a pitch as ready for betting. Use DONE to mark completion. "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "pitchId",
                        Map.of("type", "integer", "description", "The numeric pitch ID"),
                        "status",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "New status (see description for full list)",
                            "enum",
                            List.of(
                                "IDEA", "DRAFT", "SHAPED", "PENDING",
                                "STARTED", "IN_PROGRESS", "TESTING",
                                "DONE", "COOLDOWN", "CANCELLED", "CIRCUIT_BREAKER"))),
                "required",
                List.of("pitchId", "status")));
  }

  public static Map<String, Object> updatePitchDefinition() {
    return Map.of(
        "name",
        TOOL_UPDATE_PITCH,
        "description",
            "Update editable fields on an existing pitch. Only the fields you supply are changed — "
                + "omitted fields keep their current values (PATCH semantics). "
                + "To change pitch status (e.g. IDEA → SHAPED) use update_pitch_status instead. "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "pitchId",
                        Map.of("type", "integer", "description", "The numeric pitch ID (required)"),
                        "title",
                        Map.of("type", "string", "description", "New pitch title"),
                        "description",
                        Map.of("type", "string", "description", "High-level description"),
                        "problemStatement",
                        Map.of("type", "string", "description", "Shape Up: what problem does this solve?"),
                        "solution",
                        Map.of("type", "string", "description", "Shape Up: proposed solution"),
                        "rabbitHoles",
                        Map.of("type", "string", "description", "Shape Up: known rabbit holes to avoid"),
                        "risks",
                        Map.of("type", "string", "description", "Shape Up: identified risks"),
                        "noGos",
                        Map.of("type", "string", "description", "Shape Up: explicit out-of-scope items"),
                        "wireframeLinks",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "Comma-separated URLs to wireframes or Figma files"),
                        "appetiteDays",
                        Map.of(
                            "type",
                            "integer",
                            "description",
                            "Time budget in days (14 = Small Batch, 42 = Big Batch)")),
                "required",
                List.of("pitchId")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public List<McpPitchDTO> getPitches(Map<String, Object> args) {
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");

    if (cycleIdArg != null) {
      return pitchService.getPitchesByCycleId(toLong(cycleIdArg, "cycleId")).stream()
          .map(McpPitchDTO::from)
          .toList();
    }
    if (projectIdArg != null) {
      // Use accessible pitches filtered client-side by project. Pitches not yet tied to a project
      // (e.g. idea-stage pitches) have a null projectId, so compare with the boxed Long to avoid a
      // NullPointerException from auto-unboxing.
      Long projectId = toLong(projectIdArg, "projectId");
      return pitchService.getAccessiblePitches().stream()
          .filter(p -> projectId.equals(p.getProjectId()))
          .map(McpPitchDTO::from)
          .toList();
    }
    return pitchService.getAccessiblePitches().stream()
        .map(McpPitchDTO::from)
        .toList();
  }

  public McpPitchDTO getPitchDetail(Map<String, Object> args) {
    long pitchId = toLong(args.get("pitchId"), "pitchId");
    return McpPitchDTO.from(pitchService.getPitchById(pitchId));
  }

  public List<McpPitchDTO> getBettingCandidates(Map<String, Object> args) {
    Object projectIdArg = args.get("projectId");
    if (projectIdArg != null) {
      return pitchService.getBettingCandidatesByProjectId(toLong(projectIdArg, "projectId")).stream()
          .map(McpPitchDTO::from)
          .toList();
    }
    return pitchService.getBettingCandidates().stream()
        .map(McpPitchDTO::from)
        .toList();
  }

  public McpPitchDTO createPitch(Map<String, Object> args) {
    Object titleArg = args.get("title");
    if (titleArg == null || titleArg.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required argument: title");
    }

    CreatePitchRequest request = new CreatePitchRequest();
    request.setTitle(titleArg.toString().trim());

    Object problemArg = args.get("problemStatement");
    if (problemArg != null) {
      request.setProblemStatement(problemArg.toString());
    }

    Object appetiteArg = args.get("appetiteDays");
    if (appetiteArg instanceof Number n) {
      request.setAppetiteDays(n.intValue());
    } else if (appetiteArg != null) {
      try {
        request.setAppetiteDays(Integer.parseInt(appetiteArg.toString()));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("appetiteDays must be an integer, got: " + appetiteArg);
      }
    }

    Object epicIdArg = args.get("epicId");
    if (epicIdArg != null) {
      request.setEpicId(toLong(epicIdArg, "epicId"));
    }

    return McpPitchDTO.from(pitchService.createPitch(request));
  }

  public McpPitchDTO updatePitchStatus(Map<String, Object> args) {
    long pitchId = toLong(args.get("pitchId"), "pitchId");
    Object statusArg = args.get("status");
    if (statusArg == null) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    String statusStr = statusArg.toString().trim();
    if (statusStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    PitchStatus status;
    try {
      status = PitchStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid status '" + statusStr + "'. Must be one of: "
              + "IDEA, DRAFT, SHAPED, PENDING, STARTED, IN_PROGRESS, TESTING, "
              + "DONE, COOLDOWN, CANCELLED, CIRCUIT_BREAKER");
    }
    return McpPitchDTO.from(pitchService.updateStatus(pitchId, status));
  }

  public McpPitchDTO updatePitch(Map<String, Object> args) {
    long pitchId = toLong(args.get("pitchId"), "pitchId");
    PitchDTO current = pitchService.getPitchById(pitchId);

    // Seed the request with the current state to achieve PATCH semantics — updatePitch is a full replace.
    CreatePitchRequest request = new CreatePitchRequest();
    request.setTitle(current.getTitle());
    request.setDescription(current.getDescription());
    request.setAppetiteDays(current.getAppetiteDays());
    request.setStatus(current.getStatus());
    request.setCycleId(current.getCycleId());
    request.setTeamId(current.getTeamId());
    request.setEpicId(current.getEpicId());
    request.setTargetReleaseId(current.getTargetReleaseId());
    request.setPriority(current.getPriority());
    request.setSortOrder(current.getSortOrder());
    request.setProblemStatement(current.getProblemStatement());
    request.setSolution(current.getSolution());
    request.setRabbitHoles(current.getRabbitHoles());
    request.setRisks(current.getRisks());
    request.setNoGos(current.getNoGos());
    request.setWireframeLinks(current.getWireframeLinks());

    // Apply only the fields explicitly provided by the caller.
    Object titleArg = args.get("title");
    if (titleArg != null) {
      String title = titleArg.toString().trim();
      if (title.isBlank()) {
        throw new IllegalArgumentException("title cannot be blank");
      }
      request.setTitle(title);
    }
    Object descriptionArg = args.get("description");
    if (descriptionArg != null) {
      request.setDescription(descriptionArg.toString());
    }
    Object problemArg = args.get("problemStatement");
    if (problemArg != null) {
      request.setProblemStatement(problemArg.toString());
    }
    Object solutionArg = args.get("solution");
    if (solutionArg != null) {
      request.setSolution(solutionArg.toString());
    }
    Object rabbitHolesArg = args.get("rabbitHoles");
    if (rabbitHolesArg != null) {
      request.setRabbitHoles(rabbitHolesArg.toString());
    }
    Object risksArg = args.get("risks");
    if (risksArg != null) {
      request.setRisks(risksArg.toString());
    }
    Object noGosArg = args.get("noGos");
    if (noGosArg != null) {
      request.setNoGos(noGosArg.toString());
    }
    Object wireframeLinksArg = args.get("wireframeLinks");
    if (wireframeLinksArg != null) {
      request.setWireframeLinks(wireframeLinksArg.toString());
    }
    Object appetiteArg = args.get("appetiteDays");
    if (appetiteArg instanceof Number n) {
      request.setAppetiteDays(n.intValue());
    } else if (appetiteArg != null) {
      try {
        request.setAppetiteDays(Integer.parseInt(appetiteArg.toString()));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("appetiteDays must be an integer, got: " + appetiteArg);
      }
    }

    return McpPitchDTO.from(pitchService.updatePitch(pitchId, request));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

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
