package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateInitiativeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDTO;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.InitiativeDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.service.EpicDependencyService;
import com.github.farzadsedaghatbin.shipflow.service.EpicService;
import com.github.farzadsedaghatbin.shipflow.service.InitiativeService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.RoadmapService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MCP tool implementations for the roadmap: initiatives, epics, epic dependencies and the
 * timeline. Roadmap bars are epics (grouped by initiative); backlog work items are tasks.
 *
 * <p>Argument names are camelCase like every other MCP tool class; the snake_case spelling of each
 * argument (e.g. {@code project_id}) is accepted as an alias.
 */
@Component
@RequiredArgsConstructor
public class RoadmapMcpTools {

  private final RoadmapService roadmapService;
  private final EpicService epicService;
  private final InitiativeService initiativeService;
  private final EpicDependencyService epicDependencyService;
  private final TaskService taskService;
  private final ProjectService projectService;

  public static final String TOOL_GET_ROADMAP = "get_roadmap";
  public static final String TOOL_GET_EPICS = "get_epics";
  public static final String TOOL_CREATE_INITIATIVE = "create_initiative";
  public static final String TOOL_CREATE_EPIC = "create_epic";
  public static final String TOOL_UPDATE_EPIC = "update_epic";
  public static final String TOOL_ADD_EPIC_DEPENDENCY = "add_epic_dependency";
  public static final String TOOL_LINK_TASK_TO_EPIC = "link_task_to_epic";

  private static final String ROADMAP_HINT =
      "A roadmap/timeline is made of initiatives (optional groups) containing epics (the bars). ";

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static Map<String, Object> getRoadmapDefinition() {
    return Map.of(
        "name", TOOL_GET_ROADMAP,
        "description",
            "Get the roadmap timeline of a project: initiatives, epics (with their dates, progress "
                + "and child pitches for Shape Up projects or child tasks for Kanban/Scrum "
                + "projects), releases and epic-to-epic dependencies. Dependency direction: "
                + "sourceEpicId BLOCKS targetEpicId. Dates default to today-3 months .. "
                + "today+12 months.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId", Map.of("type", "integer", "description", "Project ID"),
                        "startDate", Map.of("type", "string", "description",
                            "Window start, ISO date yyyy-MM-dd (optional)"),
                        "endDate", Map.of("type", "string", "description",
                            "Window end, ISO date yyyy-MM-dd (optional)")),
                "required", List.of("projectId")));
  }

  public static Map<String, Object> getEpicsDefinition() {
    return Map.of(
        "name", TOOL_GET_EPICS,
        "description",
            "List the epics of a project with id, name, status, dates, initiative, priority and "
                + "task/pitch counts. Use this to find epic IDs before linking tasks or adding "
                + "dependencies.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of("projectId", Map.of("type", "integer", "description", "Project ID")),
                "required", List.of("projectId")));
  }

  public static Map<String, Object> createInitiativeDefinition() {
    return Map.of(
        "name", TOOL_CREATE_INITIATIVE,
        "description",
            "Create a roadmap initiative: a strategic theme that groups epics on the roadmap. "
                + ROADMAP_HINT
                + "Use this (together with create_epic) to build a roadmap or timeline from a "
                + "plan, document or image. Do NOT use create_task for roadmap items — tasks are "
                + "backlog work items. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId", Map.of("type", "integer", "description", "Project ID"),
                        "name", Map.of("type", "string", "description", "Initiative name"),
                        "description", Map.of("type", "string", "description", "Optional description"),
                        "startDate", Map.of("type", "string", "description",
                            "Target start, ISO date yyyy-MM-dd (optional)"),
                        "endDate", Map.of("type", "string", "description",
                            "Target end, ISO date yyyy-MM-dd (optional)"),
                        "color", Map.of("type", "string", "description",
                            "Hex colour like #3B82F6 (optional)")),
                "required", List.of("projectId", "name")));
  }

  public static Map<String, Object> createEpicDefinition() {
    return Map.of(
        "name", TOOL_CREATE_EPIC,
        "description",
            "Create a roadmap epic: a dated bar on the roadmap/timeline. "
                + ROADMAP_HINT
                + "This is the right tool for building a roadmap or timeline from a plan or image: "
                + "create one epic per bar with startDate/endDate (and initiativeId to group it). "
                + "create_task is for backlog work items, NOT roadmap bars. In Kanban/Scrum "
                + "projects, put tasks under an epic with link_task_to_epic or create_task's "
                + "epicId. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId", Map.of("type", "integer", "description", "Project ID"),
                        "name", Map.of("type", "string", "description", "Epic name"),
                        "description", Map.of("type", "string", "description", "Optional description"),
                        "startDate", Map.of("type", "string", "description",
                            "Target start, ISO date yyyy-MM-dd. Without dates the epic does not "
                                + "appear on the timeline."),
                        "endDate", Map.of("type", "string", "description",
                            "Target end, ISO date yyyy-MM-dd"),
                        "initiativeId", Map.of("type", "integer", "description",
                            "Parent initiative ID (optional)"),
                        "color", Map.of("type", "string", "description",
                            "Hex colour like #3B82F6 (optional)"),
                        "priority", Map.of("type", "string", "enum",
                            List.of("HIGH", "MEDIUM", "LOW"), "description", "Business value")),
                "required", List.of("projectId", "name")));
  }

  public static Map<String, Object> updateEpicDefinition() {
    return Map.of(
        "name", TOOL_UPDATE_EPIC,
        "description",
            "Update an existing roadmap epic. Only the provided fields change (PATCH semantics). "
                + "Use it to move/resize a bar (startDate/endDate), rename it, change its "
                + "status or move it to another initiative. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "epicId", Map.of("type", "integer", "description", "Epic ID"),
                        "name", Map.of("type", "string", "description", "New name"),
                        "description", Map.of("type", "string", "description", "New description"),
                        "startDate", Map.of("type", "string", "description", "ISO date yyyy-MM-dd"),
                        "endDate", Map.of("type", "string", "description", "ISO date yyyy-MM-dd"),
                        "initiativeId", Map.of("type", "integer", "description", "New parent initiative ID"),
                        "color", Map.of("type", "string", "description", "Hex colour like #3B82F6"),
                        "status", Map.of("type", "string", "enum",
                            List.of("DRAFT", "PLANNED", "IN_PROGRESS", "COMPLETED", "ON_HOLD", "CANCELLED")),
                        "priority", Map.of("type", "string", "enum", List.of("HIGH", "MEDIUM", "LOW"))),
                "required", List.of("epicId")));
  }

  public static Map<String, Object> addEpicDependencyDefinition() {
    return Map.of(
        "name", TOOL_ADD_EPIC_DEPENDENCY,
        "description",
            "Add a dependency arrow between two roadmap epics. Direction: epicId (the source) "
                + "BLOCKS targetEpicId — the target epic cannot finish/start until the source is "
                + "done. Both epics must be in the same project; circular dependencies are "
                + "rejected. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "epicId", Map.of("type", "integer", "description",
                            "Source epic: the one that blocks the target"),
                        "targetEpicId", Map.of("type", "integer", "description",
                            "Target epic: the one that is blocked / waits"),
                        "dependencyType", Map.of("type", "string", "enum",
                            List.of("BLOCKS", "DEPENDS_ON", "RELATED_TO"), "description",
                            "Default BLOCKS")),
                "required", List.of("epicId", "targetEpicId")));
  }

  public static Map<String, Object> linkTaskToEpicDefinition() {
    return Map.of(
        "name", TOOL_LINK_TASK_TO_EPIC,
        "description",
            "Put an existing task under a roadmap epic (Kanban/Scrum projects), or detach it by "
                + "omitting epicId / passing null. The epic must be in the same project as the "
                + "task. Requires WRITE API key scope.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "taskId", Map.of("type", "integer", "description", "Task ID"),
                        "epicId", Map.of("type", List.of("integer", "null"), "description",
                            "Epic ID, or null to unlink")),
                "required", List.of("taskId")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public RoadmapTimelineDTO getRoadmap(Map<String, Object> args) {
    long projectId = requiredLong(args, "projectId");
    projectService.requireProjectAccess(projectId);
    LocalDate start = optionalDate(args, "startDate");
    LocalDate end = optionalDate(args, "endDate");
    LocalDate today = LocalDate.now();
    if (start == null) {
      start = today.minusMonths(3);
    }
    if (end == null) {
      end = today.plusMonths(12);
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("startDate must not be after endDate");
    }
    return roadmapService.getRoadmapTimeline(projectId, start, end);
  }

  public List<Map<String, Object>> getEpics(Map<String, Object> args) {
    long projectId = requiredLong(args, "projectId");
    projectService.requireProjectAccess(projectId);
    return epicService.getEpicsByProjectId(projectId).stream().map(this::summarize).toList();
  }

  public InitiativeDTO createInitiative(Map<String, Object> args) {
    long projectId = requiredLong(args, "projectId");
    projectService.requireProjectAccess(projectId);
    CreateInitiativeRequest request = new CreateInitiativeRequest();
    request.setProjectId(projectId);
    request.setName(requiredString(args, "name"));
    request.setDescription(optionalString(args, "description"));
    request.setTargetStartDate(optionalDate(args, "startDate"));
    request.setTargetEndDate(optionalDate(args, "endDate"));
    request.setColor(optionalString(args, "color"));
    validateRange(request.getTargetStartDate(), request.getTargetEndDate());
    return initiativeService.createInitiative(request);
  }

  public EpicDTO createEpic(Map<String, Object> args) {
    long projectId = requiredLong(args, "projectId");
    projectService.requireProjectAccess(projectId);
    CreateEpicRequest request = new CreateEpicRequest();
    request.setProjectId(projectId);
    request.setName(requiredString(args, "name"));
    request.setDescription(optionalString(args, "description"));
    request.setTargetStartDate(optionalDate(args, "startDate"));
    request.setTargetEndDate(optionalDate(args, "endDate"));
    request.setColor(optionalString(args, "color"));
    Long initiativeId = optionalLong(args, "initiativeId");
    request.setInitiativeId(initiativeId);
    String priority = optionalString(args, "priority");
    if (priority != null) {
      request.setPriority(parseEnum(BusinessValue.class, priority, "priority"));
    }
    validateRange(request.getTargetStartDate(), request.getTargetEndDate());
    return epicService.createEpic(request);
  }

  public EpicDTO updateEpic(Map<String, Object> args) {
    long epicId = requiredLong(args, "epicId");
    EpicDTO current = epicService.getEpicById(epicId);
    projectService.requireProjectAccess(current.getProjectId());

    // EpicService.updateEpic is a full replace — start from current state for PATCH semantics.
    CreateEpicRequest request = new CreateEpicRequest();
    request.setProjectId(current.getProjectId());
    request.setName(current.getName());
    request.setDescription(current.getDescription());
    request.setStatus(current.getStatus());
    request.setColor(current.getColor());
    request.setTargetStartDate(current.getTargetStartDate());
    request.setTargetEndDate(current.getTargetEndDate());
    request.setInitiativeId(current.getInitiativeId());
    request.setOwnerId(current.getOwnerId());
    request.setSortOrder(current.getSortOrder());
    request.setPriority(current.getPriority());

    if (has(args, "name")) {
      request.setName(requiredString(args, "name"));
    }
    if (has(args, "description")) {
      request.setDescription(optionalString(args, "description"));
    }
    if (has(args, "startDate")) {
      request.setTargetStartDate(optionalDate(args, "startDate"));
    }
    if (has(args, "endDate")) {
      request.setTargetEndDate(optionalDate(args, "endDate"));
    }
    if (has(args, "initiativeId")) {
      request.setInitiativeId(optionalLong(args, "initiativeId"));
    }
    if (has(args, "color")) {
      request.setColor(optionalString(args, "color"));
    }
    if (has(args, "status")) {
      request.setStatus(parseEnum(EpicStatus.class, optionalString(args, "status"), "status"));
    }
    if (has(args, "priority")) {
      request.setPriority(parseEnum(BusinessValue.class, optionalString(args, "priority"), "priority"));
    }
    validateRange(request.getTargetStartDate(), request.getTargetEndDate());
    return epicService.updateEpic(epicId, request);
  }

  public EpicDependencyDTO addEpicDependency(Map<String, Object> args) {
    long sourceEpicId = requiredLong(args, "epicId");
    long targetEpicId = requiredLong(args, "targetEpicId");
    EpicDTO source = epicService.getEpicById(sourceEpicId);
    projectService.requireProjectAccess(source.getProjectId());
    EpicDTO target = epicService.getEpicById(targetEpicId);
    if (!source.getProjectId().equals(target.getProjectId())) {
      throw new IllegalArgumentException("Both epics must belong to the same project");
    }
    CreateEpicDependencyRequest request = new CreateEpicDependencyRequest();
    request.setTargetEpicId(targetEpicId);
    String type = optionalString(args, "dependencyType");
    request.setDependencyType(
        type != null ? parseEnum(DependencyType.class, type, "dependencyType") : DependencyType.BLOCKS);
    return epicDependencyService.addDependency(sourceEpicId, request);
  }

  public McpTaskDTO linkTaskToEpic(Map<String, Object> args) {
    long taskId = requiredLong(args, "taskId");
    TaskDTO task = taskService.getTaskById(taskId);
    if (task.getProjectId() != null) {
      projectService.requireProjectAccess(task.getProjectId());
    }
    return McpTaskDTO.from(taskService.setTaskEpic(taskId, optionalLong(args, "epicId")));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Map<String, Object> summarize(EpicDTO e) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", e.getId());
    m.put("name", e.getName());
    m.put("status", e.getStatus() != null ? e.getStatus().name() : null);
    m.put("startDate", e.getTargetStartDate());
    m.put("endDate", e.getTargetEndDate());
    m.put("initiativeId", e.getInitiativeId());
    m.put("initiativeName", e.getInitiativeName());
    m.put("priority", e.getPriority() != null ? e.getPriority().name() : null);
    m.put("pitchCount", e.getPitchCount());
    m.put("taskCount", e.getTaskCount());
    m.put("progressPercentage", e.getProgressPercentage());
    return m;
  }

  /** Looks up an argument by camelCase name, falling back to its snake_case alias. */
  static Object arg(Map<String, Object> args, String camel) {
    if (args.containsKey(camel)) {
      return args.get(camel);
    }
    return args.get(camel.replaceAll("([A-Z])", "_$1").toLowerCase());
  }

  static boolean has(Map<String, Object> args, String camel) {
    return args.containsKey(camel) || args.containsKey(camel.replaceAll("([A-Z])", "_$1").toLowerCase());
  }

  static long requiredLong(Map<String, Object> args, String name) {
    Long v = optionalLong(args, name);
    if (v == null) {
      throw new IllegalArgumentException("Missing required argument: " + name);
    }
    return v;
  }

  static Long optionalLong(Map<String, Object> args, String name) {
    Object val = arg(args, name);
    if (val == null || val.toString().isBlank() || "null".equalsIgnoreCase(val.toString())) {
      return null;
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " must be a number, got: " + val);
    }
  }

  static String requiredString(Map<String, Object> args, String name) {
    String v = optionalString(args, name);
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: " + name);
    }
    return v.trim();
  }

  static String optionalString(Map<String, Object> args, String name) {
    Object val = arg(args, name);
    return val == null ? null : val.toString();
  }

  static LocalDate optionalDate(Map<String, Object> args, String name) {
    String s = optionalString(args, name);
    if (s == null || s.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(s.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(name + " must be an ISO date (yyyy-MM-dd), got: " + s);
    }
  }

  private static void validateRange(LocalDate start, LocalDate end) {
    if (start != null && end != null && start.isAfter(end)) {
      throw new IllegalArgumentException("startDate must not be after endDate");
    }
  }

  static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String name) {
    if (value == null) {
      return null;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid " + name + " '" + value + "'. Must be one of "
              + java.util.Arrays.toString(type.getEnumConstants()));
    }
  }
}
