package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpProjectDTO;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MCP tool implementations for project operations.
 *
 * <p>All methods are executed as the authenticated user — the same access control that applies to
 * the REST API applies here. ADMIN users see all projects; others see only accessible projects.
 */
@Component
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProjectMcpTools {

  private final ProjectService projectService;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_LIST_PROJECTS = "list_projects";
  public static final String TOOL_GET_PROJECT = "get_project";

  public static Map<String, Object> listProjectsDefinition() {
    return Map.of(
        "name", TOOL_LIST_PROJECTS,
        "description",
            "List all ShipFlow projects accessible to the authenticated user. "
                + "Returns id, name, projectKey, projectType (SHAPE_UP or KANBAN), "
                + "isActive, and activeCycleCount.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()));
  }

  public static Map<String, Object> getProjectDefinition() {
    return Map.of(
        "name", TOOL_GET_PROJECT,
        "description",
            "Get details of a single ShipFlow project by ID. "
                + "Returns name, projectKey, description, projectType, isActive, and cycle counts.",
        "inputSchema",
            Map.of(
                "type", "object",
                "properties",
                    Map.of(
                        "projectId",
                        Map.of("type", "integer", "description", "The numeric project ID")),
                "required", List.of("projectId")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  public List<McpProjectDTO> listProjects() {
    return projectService.findAccessibleProjects().stream()
        .map(McpProjectDTO::from)
        .toList();
  }

  public McpProjectDTO getProject(Map<String, Object> args) {
    long projectId = toLong(args, "projectId");
    return McpProjectDTO.from(projectService.findById(projectId));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private long toLong(Map<String, Object> args, String key) {
    Object val = args.get(key);
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + key);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Argument '" + key + "' must be a number, got: " + val);
    }
  }
}
