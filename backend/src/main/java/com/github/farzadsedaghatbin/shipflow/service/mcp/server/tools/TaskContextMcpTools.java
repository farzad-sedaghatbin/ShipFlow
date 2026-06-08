package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpCycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpPitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskContextDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDetailDTO;
import com.github.farzadsedaghatbin.shipflow.service.BugReportService;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import com.github.farzadsedaghatbin.shipflow.service.TestCaseService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MCP tool that returns the full implementation context for a single task in one call.
 *
 * <p>Where {@code get_work_context} is rooted at a pitch or cycle (PM-shaped view), this tool is
 * rooted at a task — the shape a coding agent actually starts from when asked "implement T8". It
 * stitches together:
 *
 * <ul>
 *   <li>The task itself (with dependency edges, subtasks, parent task)
 *   <li>The parent pitch (Shape Up problem/solution/rabbitHoles/risks/noGos + wireframe URLs)
 *   <li>The parent cycle metadata
 *   <li>Sibling tasks under the same pitch (capped)
 *   <li>A small {@code hints} array that tells the agent how to use the payload — e.g. that the
 *       pitch carries a Figma URL, or that the task is currently blocked
 * </ul>
 *
 * <p>The {@code hints} array is the cheap closure on the "URL exposed, not pixels" gap: when a
 * pitch has {@code wireframeLinks}, the hint tells the agent to fetch the design via a Figma MCP.
 */
@Component
@RequiredArgsConstructor
public class TaskContextMcpTools {

  /** Default sibling cap when caller does not specify one. */
  static final int DEFAULT_SIBLING_LIMIT = 50;

  /** Hard upper bound — protects payload size even if a caller asks for more. */
  static final int MAX_SIBLING_LIMIT = 200;

  public static final String TOOL_GET_TASK_CONTEXT = "get_task_context";

  private final TaskService taskService;
  private final PitchService pitchService;
  private final CycleService cycleService;
  private final TestCaseService testCaseService;
  private final BugReportService bugReportService;

  public static Map<String, Object> getTaskContextDefinition() {
    return Map.of(
        "name", TOOL_GET_TASK_CONTEXT,
        "description",
            "Return everything a coding agent needs to implement a single task in one call. "
                + "Includes: the task (with dependency graph and subtasks), its parent pitch "
                + "(Shape Up problem/solution/rabbitHoles/risks/noGos and wireframeLinks — "
                + "follow up with Figma MCP if present), the parent cycle, sibling tasks under "
                + "the same pitch (capped at 50 by default, max 200), and a 'hints' array that "
                + "describes how to use the payload. Prefer this over get_task + get_pitch_detail "
                + "+ get_tasks when the goal is to implement a specific task.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of(
                            "type", "integer",
                            "description", "The numeric task ID (required)"),
                        "includeSiblings",
                        Map.of(
                            "type", "boolean",
                            "description",
                                "Include sibling tasks under the same pitch. Default true."),
                        "siblingLimit",
                        Map.of(
                            "type", "integer",
                            "description",
                                "Cap on sibling tasks returned. Default 50, max 200. The "
                                    + "'siblingsTruncated' flag in the response indicates whether "
                                    + "more tasks exist than were returned.")),
                "required", List.of("taskId")));
  }

  // ── Implementation ────────────────────────────────────────────────────────

  public McpTaskContextDTO getTaskContext(Map<String, Object> args) {
    long taskId = toLong(args.get("taskId"), "taskId");
    boolean includeSiblings = !Boolean.FALSE.equals(args.get("includeSiblings"));
    int siblingLimit = resolveSiblingLimit(args.get("siblingLimit"));

    TaskDTO task = taskService.getTaskById(taskId);
    McpTaskDetailDTO detail = McpTaskDetailDTO.from(task);

    // QA / bug counts — cheap repo COUNT queries; null-tolerant if the feature is disabled.
    int testCaseCount = safeCount(() -> (int) testCaseService.countTestCasesByTask(taskId));
    int bugReportCount = safeCount(() -> (int) bugReportService.countBugReportsByTask(taskId));
    detail.setTestCaseCount(testCaseCount);
    detail.setBugReportCount(bugReportCount);

    McpPitchDTO pitch = null;
    PitchDTO rawPitch = null;
    if (task.getPitchId() != null) {
      rawPitch = pitchService.getPitchById(task.getPitchId());
      pitch = McpPitchDTO.from(rawPitch);
    }

    McpCycleDTO cycle = task.getCycleId() != null
        ? McpCycleDTO.from(cycleService.getCycleById(task.getCycleId()))
        : null;

    List<McpTaskDTO> siblings = List.of();
    Map<String, Integer> siblingStatusCounts = Map.of();
    boolean truncated = false;
    int siblingTotalCount = 0;
    if (includeSiblings) {
      List<TaskDTO> rawSiblings = fetchSiblings(task);
      siblingTotalCount = rawSiblings.size();
      truncated = rawSiblings.size() > siblingLimit;
      List<TaskDTO> capped = truncated ? rawSiblings.subList(0, siblingLimit) : rawSiblings;
      siblings = capped.stream().map(McpTaskDTO::from).toList();
      siblingStatusCounts = countByStatus(siblings);
    }

    List<String> hints = buildHints(
        task, rawPitch, siblingTotalCount, truncated, testCaseCount, bugReportCount);

    return McpTaskContextDTO.builder()
        .task(detail)
        .pitch(pitch)
        .cycle(cycle)
        .siblings(siblings)
        .siblingsTruncated(truncated)
        .siblingTotalCount(siblingTotalCount)
        .siblingStatusCounts(siblingStatusCounts)
        .hints(hints)
        .build();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Siblings = other tasks under the same pitch. Falls back to other tasks in the same cycle when
   * the focal task has no pitch (Kanban-style). The focal task itself is filtered out.
   */
  private List<TaskDTO> fetchSiblings(TaskDTO task) {
    List<TaskDTO> raw;
    if (task.getPitchId() != null) {
      raw = taskService.getTasksByPitchId(task.getPitchId());
    } else if (task.getCycleId() != null) {
      raw = taskService.getTasksByCycleId(task.getCycleId());
    } else {
      return List.of();
    }
    return raw.stream().filter(t -> !t.getId().equals(task.getId())).toList();
  }

  private List<String> buildHints(
      TaskDTO task,
      PitchDTO pitch,
      int siblingTotalCount,
      boolean truncated,
      int testCaseCount,
      int bugReportCount) {
    List<String> hints = new ArrayList<>();

    if (testCaseCount > 0) {
      hints.add(
          "task has " + testCaseCount + " test case(s) attached — call get_test_cases(taskId: "
              + task.getId() + ") for the acceptance criteria, and record_test_run after"
              + " verifying.");
    }

    if (bugReportCount > 0) {
      hints.add(
          "task has " + bugReportCount + " bug report(s) linked — call get_bug_reports(taskId: "
              + task.getId() + ") to read symptoms and reproduction steps.");
    }

    if (pitch != null
        && pitch.getWireframeLinks() != null
        && !pitch.getWireframeLinks().isBlank()) {
      hints.add(
          "pitch.wireframeLinks is present — fetch the design via a Figma MCP (or follow the URL"
              + " in another tool) before implementing.");
    }

    if (Boolean.TRUE.equals(task.getIsBlocked())) {
      int count = task.getBlockedByCount() != null ? task.getBlockedByCount() : 0;
      hints.add(
          "task is BLOCKED by " + count + " task(s) — resolve dependencies before starting; see"
              + " task.blockedByTasks for IDs.");
    }

    if (pitch != null && isBlank(pitch.getSolution())) {
      hints.add(
          "pitch.solution is empty — Shape Up context is thin; consider asking for clarification"
              + " before implementing.");
    }

    if (task.getChildren() != null && !task.getChildren().isEmpty()) {
      hints.add(
          "task has " + task.getChildren().size() + " subtask(s) — consider implementing those"
              + " first; see task.children.");
    }

    if (task.getPitchId() == null) {
      hints.add(
          "task is not linked to a pitch — no Shape Up context available; siblings (if any) come"
              + " from the same cycle.");
    }

    if (truncated) {
      hints.add(
          "siblings list was truncated; " + siblingTotalCount + " total tasks exist under this"
              + " pitch. Raise siblingLimit or call get_tasks for the full list.");
    }

    return hints;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  /**
   * Run a count query and swallow expected feature-flag errors so an aggregator call doesn't
   * fail just because the QA module happens to be disabled on this instance.
   */
  private static int safeCount(java.util.function.IntSupplier supplier) {
    try {
      return supplier.getAsInt();
    } catch (RuntimeException e) {
      return 0;
    }
  }

  private static Map<String, Integer> countByStatus(List<McpTaskDTO> tasks) {
    Map<String, Integer> counts = new HashMap<>();
    for (McpTaskDTO t : tasks) {
      String key = t.getStatus() != null ? t.getStatus() : "UNKNOWN";
      counts.merge(key, 1, Integer::sum);
    }
    return counts;
  }

  private static int resolveSiblingLimit(Object arg) {
    if (arg == null) {
      return DEFAULT_SIBLING_LIMIT;
    }
    int requested;
    if (arg instanceof Number n) {
      requested = n.intValue();
    } else {
      try {
        requested = Integer.parseInt(arg.toString());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Argument 'siblingLimit' must be a number, got: " + arg);
      }
    }
    if (requested <= 0) {
      return DEFAULT_SIBLING_LIMIT;
    }
    return Math.min(requested, MAX_SIBLING_LIMIT);
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
