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
  public static final String TOOL_CREATE_TASK = "create_task";

  public static Map<String, Object> getTasksDefinition() {
    return Map.of(
        "name", TOOL_GET_TASKS,
        "description",
            "List tasks for a cycle or project. Returns id, title, status "
                + "(TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED), priority, assigneeName, "
                + "pitchTitle, isBlocked, blockedByCount, and dueDate. "
                + "One of cycleId or projectId is REQUIRED (cycleId takes precedence). "
                + "Omitting both is rejected to prevent unscoped enumeration.",
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
                            "description", "Filter tasks by project ID")),
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
            "Create a new task inside a cycle. Returns the created task. "
                + "Requires WRITE API key scope.",
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

  public List<McpTaskDTO> getTasks(Map<String, Object> args) {
    Object cycleIdArg = args.get("cycleId");
    Object projectIdArg = args.get("projectId");

    if (cycleIdArg != null) {
      return taskService.getTasksByCycleId(toLong(cycleIdArg)).stream()
          .map(McpTaskDTO::from)
          .toList();
    }
    if (projectIdArg != null) {
      return taskService.getTasksByProjectId(toLong(projectIdArg)).stream()
          .map(McpTaskDTO::from)
          .toList();
    }
    throw new IllegalArgumentException(
        "Either 'cycleId' or 'projectId' must be provided. "
            + "Listing all tasks across projects is not permitted via MCP.");
  }

  public McpTaskDTO getTask(Map<String, Object> args) {
    long taskId = toLong(args.get("taskId"));
    return McpTaskDTO.from(taskService.getTaskById(taskId));
  }

  public List<McpTaskDTO> getBlockers(Map<String, Object> args) {
    return getTasks(args).stream()
        .filter(t -> Boolean.TRUE.equals(t.getIsBlocked()))
        .toList();
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
