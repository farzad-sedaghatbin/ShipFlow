package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** MCP tool implementations for task operations (read and write). */
@Component
@RequiredArgsConstructor
public class TaskMcpTools {

  private final TaskService taskService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_GET_TASKS = "get_tasks";
  public static final String TOOL_GET_TASK = "get_task";
  public static final String TOOL_GET_BLOCKERS = "get_blockers";
  public static final String TOOL_UPDATE_TASK_STATUS = "update_task_status";
  public static final String TOOL_UPDATE_TASK_ASSIGNEE = "update_task_assignee";
  public static final String TOOL_CREATE_TASK = "create_task";

  public static Map<String, Object> getTasksDefinition() {
    return Map.of(
        "name", TOOL_GET_TASKS,
        "description",
            "List tasks with one or more filters. Returns id, title, status "
                + "(TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED), priority, assigneeName, pitchId, "
                + "pitchTitle, isBlocked, blockedByCount, and dueDate. "
                + "At least one scope is REQUIRED: cycleId, projectId, pitchId, assigneeId, or "
                + "mine=true. Omitting all scopes is rejected to prevent unscoped enumeration. "
                + "Filters compose (e.g. cycleId + mine returns your tasks in that cycle); "
                + "precedence when multiple scopes are given: cycleId > projectId > pitchId > "
                + "assigneeId/mine.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "cycleId",
                        Map.of(
                            "type", "integer",
                            "description", "Filter tasks by cycle ID"),
                        "projectId",
                        Map.of(
                            "type", "integer",
                            "description", "Filter tasks by project ID"),
                        "pitchId",
                        Map.of(
                            "type", "integer",
                            "description", "Filter tasks linked to this pitch"),
                        "assigneeId",
                        Map.of(
                            "type", "integer",
                            "description",
                                "Filter to tasks assigned to this person (personId, not userId — "
                                    + "get it from whoami or a person lookup)"),
                        "mine",
                        Map.of(
                            "type", "boolean",
                            "description",
                                "When true, filter to tasks assigned to the authenticated MCP "
                                    + "user. Equivalent to assigneeId=<my personId> without the "
                                    + "extra whoami round-trip.")),
                "required", List.of()));
  }

  public static Map<String, Object> getTaskDefinition() {
    return Map.of(
        "name", TOOL_GET_TASK,
        "description",
            "Get full details of a single task by ID, including dependencies, "
                + "blocked-by relationships, and comments count.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of("type", "integer", "description", "The numeric task ID")),
                "required", List.of("taskId")));
  }

  public static Map<String, Object> getBlockersDefinition() {
    return Map.of(
        "name", TOOL_GET_BLOCKERS,
        "description",
            "List all tasks that are currently blocked (isBlocked=true) within a cycle or project. "
                + "Useful for identifying what's stopping the team from making progress.",
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

  public static Map<String, Object> createTaskDefinition() {
    return Map.of(
        "name",
        TOOL_CREATE_TASK,
        "description",
            "Create a new task inside a cycle, optionally linked to a pitch or nested under a "
                + "parent task. Returns the created task. Supply pitchId to wire the task into a "
                + "Shape Up pitch — this populates the pitch dropdown in the UI and associates the "
                + "task with the pitch's hill-chart scope. Supply parentTaskId to create the task "
                + "as a subtask of another task (it will appear under the parent in the task tree "
                + "and in get_task_context's task.children list). Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "cycleId",
                        Map.of("type", "integer", "description", "ID of the cycle to add the task to"),
                        "title",
                        Map.of("type", "string", "description", "Task title (required)"),
                        "description",
                        Map.of("type", "string", "description", "Optional task description"),
                        "pitchId",
                        Map.of(
                            "type", "integer",
                            "description",
                                "Optional pitch ID to link this task to. The pitch must be assigned "
                                    + "to the same cycle (PENDING or later status)."),
                        "parentTaskId",
                        Map.of(
                            "type", "integer",
                            "description",
                                "Optional parent task ID. When set, the created task is a subtask "
                                    + "of that parent and will appear under it in the task tree. "
                                    + "Use this to break a task down into checklist-style children."),
                        "assigneeUsername",
                        Map.of("type", "string", "description", "Optional username to assign the task to"),
                        "priority",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "Task priority: LOW, MEDIUM, HIGH, URGENT (default MEDIUM)",
                            "enum",
                            List.of("LOW", "MEDIUM", "HIGH", "URGENT"))),
                "required",
                List.of("cycleId", "title")));
  }

  public static Map<String, Object> updateTaskAssigneeDefinition() {
    return Map.of(
        "name", TOOL_UPDATE_TASK_ASSIGNEE,
        "description",
            "Assign a task to a person, or unassign it. Specify exactly one of: "
                + "assigneeUsername (looked up server-side), assigneeId (personId, e.g. from "
                + "whoami), mine=true (assign to the authenticated MCP user), or unassign=true "
                + "(clear the assignee). Status, pitch, dependencies, and other fields are not "
                + "touched. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of("type", "integer", "description", "The numeric task ID"),
                        "assigneeUsername",
                        Map.of(
                            "type", "string",
                            "description", "Username of the person to assign to"),
                        "assigneeId",
                        Map.of(
                            "type", "integer",
                            "description", "personId of the person to assign to"),
                        "mine",
                        Map.of(
                            "type", "boolean",
                            "description",
                                "When true, assign to the authenticated MCP user — no separate "
                                    + "whoami round-trip needed."),
                        "unassign",
                        Map.of(
                            "type", "boolean",
                            "description",
                                "When true, clear the task's assignee. Mutually exclusive with "
                                    + "the assign options.")),
                "required", List.of("taskId")));
  }

  public static Map<String, Object> updateTaskStatusDefinition() {
    return Map.of(
        "name", TOOL_UPDATE_TASK_STATUS,
        "description",
            "Update the status of a task. Valid statuses: TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED. "
                + "Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId",
                        Map.of("type", "integer", "description", "The numeric task ID"),
                        "status",
                        Map.of(
                            "type", "string",
                            "description",
                                "New status: TODO, IN_PROGRESS, IN_REVIEW, DONE, or BLOCKED",
                            "enum",
                                List.of("TODO", "IN_PROGRESS", "IN_REVIEW", "DONE", "BLOCKED"))),
                "required", List.of("taskId", "status")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public List<McpTaskDTO> getTasks(Map<String, Object> args, Authentication auth) {
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");
    Object pitchIdArg = args.get("pitchId");
    Object assigneeIdArg = args.get("assigneeId");
    boolean mine = Boolean.TRUE.equals(args.get("mine"));

    if (cycleIdArg == null && projectIdArg == null && pitchIdArg == null
        && assigneeIdArg == null && !mine) {
      throw new IllegalArgumentException(
          "At least one scope is required: cycleId, projectId, pitchId, assigneeId, or mine=true. "
              + "Listing all tasks across projects is not permitted via MCP.");
    }

    // Resolve 'mine' to a personId once — used as a post-filter when combined with other scopes.
    Long minePersonId = null;
    if (mine) {
      User caller = resolveUser(auth);
      if (caller.getPerson() == null) {
        throw new IllegalArgumentException(
            "MCP user '" + caller.getUsername() + "' has no linked person profile; "
                + "cannot resolve 'mine'.");
      }
      minePersonId = caller.getPerson().getId();
    }

    // Resolve the primary scope (cycle > project > pitch > assignee/mine).
    List<TaskDTO> tasks;
    if (cycleIdArg != null) {
      tasks = taskService.getTasksByCycleId(toLong(cycleIdArg));
    } else if (projectIdArg != null) {
      tasks = taskService.getTasksByProjectId(toLong(projectIdArg));
    } else if (pitchIdArg != null) {
      tasks = taskService.getTasksByPitchId(toLong(pitchIdArg));
    } else if (assigneeIdArg != null) {
      tasks = taskService.getTasksByPersonId(toLong(assigneeIdArg));
    } else {
      // mine=true with no other scope — caller's tasks across all accessible projects.
      tasks = taskService.getTasksByPersonId(minePersonId);
    }

    // Compose extra filters that weren't the primary scope.
    if (pitchIdArg != null && (cycleIdArg != null || projectIdArg != null)) {
      long pitchId = toLong(pitchIdArg);
      tasks = tasks.stream().filter(t -> pitchId == nullSafe(t.getPitchId())).toList();
    }
    if (assigneeIdArg != null
        && (cycleIdArg != null || projectIdArg != null || pitchIdArg != null)) {
      long assigneeId = toLong(assigneeIdArg);
      tasks = tasks.stream().filter(t -> assigneeId == nullSafe(t.getAssigneeId())).toList();
    }
    if (mine && (cycleIdArg != null || projectIdArg != null
        || pitchIdArg != null || assigneeIdArg != null)) {
      final long pid = minePersonId;
      tasks = tasks.stream().filter(t -> pid == nullSafe(t.getAssigneeId())).toList();
    }

    return tasks.stream().map(McpTaskDTO::from).toList();
  }

  private static long nullSafe(Long val) {
    return val == null ? Long.MIN_VALUE : val;
  }

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsernameWithPerson(auth.getName())
        .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  public McpTaskDTO getTask(Map<String, Object> args) {
    long taskId = toLong(args.get("taskId"));
    return McpTaskDTO.from(taskService.getTaskById(taskId));
  }

  public List<McpTaskDTO> getBlockers(Map<String, Object> args, Authentication auth) {
    return getTasks(args, auth).stream()
        .filter(t -> Boolean.TRUE.equals(t.getIsBlocked()))
        .toList();
  }

  public McpTaskDTO updateTaskAssignee(Map<String, Object> args, Authentication auth) {
    long taskId = toLong(args.get("taskId"));
    Object usernameArg = args.get("assigneeUsername");
    Object assigneeIdArg = args.get("assigneeId");
    boolean mine = Boolean.TRUE.equals(args.get("mine"));
    boolean unassign = Boolean.TRUE.equals(args.get("unassign"));

    // Exactly-one validation — empty strings count as "not provided".
    boolean usernameSet = usernameArg != null && !usernameArg.toString().isBlank();
    int optionsSet =
        (usernameSet ? 1 : 0)
            + (assigneeIdArg != null ? 1 : 0)
            + (mine ? 1 : 0)
            + (unassign ? 1 : 0);
    if (optionsSet == 0) {
      throw new IllegalArgumentException(
          "Specify one of: assigneeUsername, assigneeId, mine=true, or unassign=true.");
    }
    if (optionsSet > 1) {
      throw new IllegalArgumentException(
          "Only one of assigneeUsername, assigneeId, mine, unassign may be set.");
    }

    Long personId;
    if (unassign) {
      personId = null;
    } else if (mine) {
      User caller = resolveUser(auth);
      if (caller.getPerson() == null) {
        throw new IllegalArgumentException(
            "MCP user '" + caller.getUsername() + "' has no linked person profile.");
      }
      personId = caller.getPerson().getId();
    } else if (assigneeIdArg != null) {
      personId = toLong(assigneeIdArg);
    } else {
      User assignee = userRepository.findByUsernameWithPerson(usernameArg.toString())
          .orElseThrow(() -> new IllegalArgumentException(
              "User not found: " + usernameArg));
      if (assignee.getPerson() == null) {
        throw new IllegalArgumentException(
            "User '" + usernameArg + "' has no linked person profile.");
      }
      personId = assignee.getPerson().getId();
    }

    TaskDTO updated = taskService.updateTaskAssignee(taskId, personId);
    return McpTaskDTO.from(updated);
  }

  public McpTaskDTO updateTaskStatus(Map<String, Object> args) {
    long taskId = toLong(args.get("taskId"));
    String statusStr = (String) args.get("status");
    if (statusStr == null || statusStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: status");
    }
    TaskStatus status;
    try {
      status = TaskStatus.valueOf(statusStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid status '" + statusStr + "'. Must be one of: TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED");
    }
    TaskDTO updated = taskService.updateTaskStatus(taskId, status);
    return McpTaskDTO.from(updated);
  }

  public McpTaskDTO createTask(Map<String, Object> args) {
    Object cycleIdArg = args.get("cycleId");
    if (cycleIdArg == null) {
      throw new IllegalArgumentException("Missing required argument: cycleId");
    }
    Object titleArg = args.get("title");
    if (titleArg == null || titleArg.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required argument: title");
    }

    CreateTaskRequest request = new CreateTaskRequest();
    request.setCycleId(toLong(cycleIdArg));
    request.setTitle(titleArg.toString().trim());

    Object descriptionArg = args.get("description");
    if (descriptionArg != null) {
      request.setDescription(descriptionArg.toString());
    }

    Object priorityArg = args.get("priority");
    if (priorityArg != null) {
      try {
        request.setPriority(TaskPriority.valueOf(priorityArg.toString().toUpperCase()));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid priority '" + priorityArg + "'. Must be LOW, MEDIUM, HIGH, or URGENT");
      }
    }

    Object pitchIdArg = args.get("pitchId");
    if (pitchIdArg != null) {
      request.setPitchId(toLong(pitchIdArg));
    }

    Object parentTaskIdArg = args.get("parentTaskId");
    if (parentTaskIdArg != null) {
      request.setParentTaskId(toLong(parentTaskIdArg));
    }

    Object assigneeUsernameArg = args.get("assigneeUsername");
    if (assigneeUsernameArg != null && !assigneeUsernameArg.toString().isBlank()) {
      Optional<User> assignee = userRepository.findByUsernameWithPerson(assigneeUsernameArg.toString());
      if (assignee.isPresent() && assignee.get().getPerson() != null) {
        request.setAssigneeId(assignee.get().getPerson().getId());
      }
    }

    TaskDTO created = taskService.createTask(request);
    return McpTaskDTO.from(created);
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
