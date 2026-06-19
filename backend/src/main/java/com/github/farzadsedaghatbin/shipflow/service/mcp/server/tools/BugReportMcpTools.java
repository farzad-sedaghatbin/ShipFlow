package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpBugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.BugReportService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * MCP tools for the bug-tracking domain.
 *
 * <p>The {@code add_comment} tool already accepts {@code entityType: BUG_REPORT}, so an agent could
 * comment on a bug it had no way to read. This class closes that asymmetry: read bugs by task,
 * pitch, or cycle; read a single bug; update bug status.
 */
@Component
@RequiredArgsConstructor
public class BugReportMcpTools {

  public static final String TOOL_GET_BUG_REPORTS = "get_bug_reports";
  public static final String TOOL_GET_BUG_REPORT = "get_bug_report";
  public static final String TOOL_UPDATE_BUG_STATUS = "update_bug_status";

  private final BugReportService bugReportService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static Map<String, Object> getBugReportsDefinition() {
    return Map.of(
        "name", TOOL_GET_BUG_REPORTS,
        "description",
            "List bug reports. Filter by projectId, taskId, pitchId, or cycleId — all optional; "
                + "omitting all returns the most-recent 50 bugs across the workspace. "
                + "Use the 'search' param to match title, description, or bugKey. "
                + "Returns bugKey, title, severity (TRIVIAL/MINOR/MAJOR/CRITICAL/BLOCKER), "
                + "status (OPEN, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE), "
                + "reproduction steps, expected vs actual behaviour, and assignee.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId",
                        Map.of("type", "integer", "description", "All bugs in this project"),
                        "taskId",
                        Map.of("type", "integer", "description", "Bugs linked to this task"),
                        "pitchId",
                        Map.of("type", "integer", "description", "Bugs linked to this pitch"),
                        "cycleId",
                        Map.of("type", "integer", "description", "All bugs in this cycle"),
                        "search",
                        Map.of("type", "string",
                            "description", "Free-text search against title, description, and bugKey")),
                "required", List.of()));
  }

  public static Map<String, Object> getBugReportDefinition() {
    return Map.of(
        "name", TOOL_GET_BUG_REPORT,
        "description", "Get full details of a single bug report by ID.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "bugReportId",
                        Map.of("type", "integer", "description", "Numeric bug report ID")),
                "required", List.of("bugReportId")));
  }

  public static Map<String, Object> updateBugStatusDefinition() {
    return Map.of(
        "name", TOOL_UPDATE_BUG_STATUS,
        "description",
            "Update the status of a bug report. Valid statuses: OPEN, IN_PROGRESS, RESOLVED, "
                + "VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE. Transitioning to RESOLVED/"
                + "VERIFIED/CLOSED stamps the resolvedAt timestamp automatically. Optional "
                + "resolution text captures how it was fixed. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "bugReportId",
                        Map.of("type", "integer", "description", "ID of the bug to update"),
                        "status",
                        Map.of(
                            "type", "string",
                            "description", "New bug status",
                            "enum",
                                List.of(
                                    "OPEN",
                                    "IN_PROGRESS",
                                    "RESOLVED",
                                    "VERIFIED",
                                    "CLOSED",
                                    "REOPENED",
                                    "WONT_FIX",
                                    "DUPLICATE")),
                        "resolution",
                        Map.of(
                            "type", "string",
                            "description",
                                "Optional resolution text — what the fix was, why it was closed, "
                                    + "or why it's WONT_FIX/DUPLICATE.")),
                "required", List.of("bugReportId", "status")));
  }

  // ── Implementations ──────────────────────────────────────────────────────

  public List<McpBugReportDTO> getBugReports(Map<String, Object> args) {
    Object taskIdArg = args.get("taskId");
    Object pitchIdArg = args.get("pitchId");
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");
    String search = args.get("search") instanceof String s ? s.trim() : null;

    // Fast-path: single-scope queries that don't need search
    if (search == null || search.isBlank()) {
      if (taskIdArg != null) {
        return bugReportService.getBugReportsByTask(toLong(taskIdArg, "taskId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
      if (pitchIdArg != null) {
        return bugReportService.getBugReportsByPitch(toLong(pitchIdArg, "pitchId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
      if (cycleIdArg != null) {
        return bugReportService.getBugReportsByCycle(toLong(cycleIdArg, "cycleId")).stream()
            .map(McpBugReportDTO::from).toList();
      }
    }

    // Filtered query: supports projectId, cycleId, pitchId, and search
    Long projectId = projectIdArg != null ? toLong(projectIdArg, "projectId") : null;
    Long cycleId = cycleIdArg != null ? toLong(cycleIdArg, "cycleId") : null;
    Long pitchId = pitchIdArg != null ? toLong(pitchIdArg, "pitchId") : null;
    String effectiveSearch = (search != null && !search.isBlank()) ? search : null;

    var pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
    return bugReportService
        .getBugReportsWithFilters(projectId, cycleId, pitchId, null, null, null, false,
            effectiveSearch, pageable)
        .getContent()
        .stream()
        .map(McpBugReportDTO::from)
        .toList();
  }

  public McpBugReportDTO getBugReport(Map<String, Object> args) {
    long id = toLong(args.get("bugReportId"), "bugReportId");
    return McpBugReportDTO.from(bugReportService.getBugReportById(id));
  }

  public McpBugReportDTO updateBugStatus(Map<String, Object> args, Authentication auth) {
    long bugReportId = toLong(args.get("bugReportId"), "bugReportId");
    String statusStr = (String) args.get("status");
    if (statusStr == null || statusStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    BugStatus status;
    try {
      status = BugStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid status '" + statusStr + "'. Must be one of: OPEN, IN_PROGRESS, RESOLVED, "
              + "VERIFIED, CLOSED, REOPENED, WONT_FIX, DUPLICATE");
    }

    User caller = resolveUser(auth);

    UpdateBugReportRequest request = new UpdateBugReportRequest();
    request.setStatus(status);
    if (args.get("resolution") != null) {
      request.setResolution(args.get("resolution").toString());
    }

    BugReportDTO updated = bugReportService.updateBugReport(bugReportId, request, caller.getId());
    return McpBugReportDTO.from(updated);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private static long toLong(Object val, String argName) {
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
