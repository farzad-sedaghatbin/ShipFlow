package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CommentMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CycleMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.PitchMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.ProjectMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WiseArchitectureMcpTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Routes incoming JSON-RPC 2.0 messages to the appropriate MCP tool implementation and sends the
 * response back through the caller's SSE stream.
 *
 * <p>Supported JSON-RPC methods:
 *
 * <ul>
 *   <li>{@code initialize} — returns server capabilities
 *   <li>{@code notifications/initialized} — client confirmation, no response
 *   <li>{@code ping} — health check
 *   <li>{@code tools/list} — returns all available tool definitions
 *   <li>{@code tools/call} — invokes a named tool and returns the result
 * </ul>
 *
 * <p>Write tools ({@code update_task_status}) require the API key to have {@code SCOPE_WRITE} or
 * {@code SCOPE_ADMIN} authority. Read tools are available to any authenticated key.
 */
@Service
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class McpToolDispatcher {

  public static final String PROTOCOL_VERSION = "2024-11-05";

  private final McpSessionManager sessionManager;
  private final McpServerProperties properties;
  private final ObjectMapper objectMapper;

  private final ProjectMcpTools projectTools;
  private final CycleMcpTools cycleTools;
  private final TaskMcpTools taskTools;
  private final PitchMcpTools pitchTools;
  private final CommentMcpTools commentTools;
  private final WiseArchitectureMcpTools wiseArchitectureTools;

  /**
   * Names of all write tools, derived once from {@link #writeTools()} at construction time.
   * Allows O(1) membership checks in {@link #isWriteTool(String)} without rebuilding the
   * definition list on every {@code tools/call} request.
   */
  private static final Set<String> WRITE_TOOL_NAMES =
      writeToolDefinitions().stream()
          .map(def -> (String) def.get("name"))
          .collect(Collectors.toUnmodifiableSet());

  /**
   * Process a JSON-RPC 2.0 request from a given session and send the response via SSE.
   *
   * @param sessionId the session that sent the message
   * @param request the parsed JSON-RPC request body
   */
  @SuppressWarnings("unchecked")
  public void dispatch(String sessionId, Map<String, Object> request) {
    Object id = request.get("id");
    String method = (String) request.get("method");
    Map<String, Object> params =
        request.get("params") instanceof Map ? (Map<String, Object>) request.get("params") : Map.of();

    log.debug("MCP dispatch: session={} method={} id={}", sessionId, method, id);

    try {
      Object result = switch (method) {
        case "initialize" -> handleInitialize(params);
        case "notifications/initialized" -> {
          // Client confirms initialization — no response needed
          log.debug("MCP session {} initialized by client", sessionId);
          yield null;
        }
        case "ping" -> Map.of();
        case "tools/list" -> handleToolsList(sessionId);
        case "tools/call" -> handleToolCall(sessionId, params);
        default -> {
          log.warn("Unknown MCP method [session={}]: {}", sessionId, method);
          if (id != null) {
            // JSON-RPC 2.0 §5.1: unknown method must return -32601 so clients don't hang
            trySendError(sessionId, id, -32601, "Method not found: " + method);
          }
          yield null;
        }
      };

      // Notifications (id == null) and unknown-method cases (result == null) need no success reply
      if (result == null || id == null) {
        return;
      }

      sessionManager.send(sessionId, jsonRpcSuccess(id, result));

    } catch (McpToolException e) {
      log.warn("MCP tool error [session={} method={}]: {}", sessionId, method, e.getMessage());
      trySendError(sessionId, id, -32602, e.getMessage());
    } catch (SecurityException e) {
      log.warn("MCP auth error [session={} method={}]: {}", sessionId, method, e.getMessage());
      trySendError(sessionId, id, -32000, e.getMessage());
    } catch (Exception e) {
      log.error("MCP internal error [session={} method={}]", sessionId, method, e);
      trySendError(sessionId, id, -32603, "Internal error: " + e.getMessage());
    }
  }

  // ── JSON-RPC method handlers ───────────────────────────────────────────────

  private Map<String, Object> handleInitialize(Map<String, Object> params) {
    return Map.of(
        "protocolVersion", PROTOCOL_VERSION,
        "capabilities",
            Map.of(
                "tools", Map.of("listChanged", false),
                "resources", Map.of()),
        "serverInfo",
            Map.of(
                "name", properties.getServerName(),
                "version", "0.7.0"));
  }

  private Map<String, Object> handleToolsList(String sessionId) {
    Authentication auth = sessionManager.get(sessionId)
        .map(McpSession::getAuth)
        .orElseThrow(() -> new McpToolException("Session not found"));

    List<Map<String, Object>> tools = new ArrayList<>(readTools());
    if (properties.isWriteEnabled() && hasWriteScope(auth)) {
      tools.addAll(writeTools());
    }
    return Map.of("tools", tools);
  }

  @SuppressWarnings("unchecked")
  private Object handleToolCall(String sessionId, Map<String, Object> params) {
    String toolName = (String) params.get("name");
    Map<String, Object> args = params.get("arguments") instanceof Map
        ? (Map<String, Object>) params.get("arguments")
        : Map.of();

    if (toolName == null || toolName.isBlank()) {
      throw new McpToolException("Missing tool name in tools/call params");
    }

    // Resolve session auth once — used for both security gate and tool dispatch
    Authentication auth = sessionManager.get(sessionId)
        .map(McpSession::getAuth)
        .orElseThrow(() -> new SecurityException("Session not found"));

    // Enforce write scope for mutating tools
    if (isWriteTool(toolName)) {
      if (!properties.isWriteEnabled()) {
        throw new SecurityException("Write tools are disabled on this ShipFlow instance. "
            + "Set MCP_SERVER_WRITE_ENABLED=true to enable them.");
      }
      if (!hasWriteScope(auth)) {
        throw new SecurityException("This API key does not have WRITE scope. "
            + "Generate a new key with WRITE permission in ShipFlow Settings → API Keys.");
      }
    }

    Object result = dispatchTool(toolName, args, auth);
    String json;
    try {
      json = objectMapper.writeValueAsString(result);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialise tool result", e);
    }

    return Map.of(
        "content", List.of(Map.of("type", "text", "text", json)),
        "isError", false);
  }

  // ── Tool dispatch ─────────────────────────────────────────────────────────

  private Object dispatchTool(String name, Map<String, Object> args, Authentication auth) {
    return switch (name) {
      // Project tools
      case ProjectMcpTools.TOOL_LIST_PROJECTS -> projectTools.listProjects();
      case ProjectMcpTools.TOOL_GET_PROJECT -> projectTools.getProject(args);

      // Cycle tools
      case CycleMcpTools.TOOL_GET_CYCLES -> cycleTools.getCycles(args);
      case CycleMcpTools.TOOL_GET_CYCLE -> cycleTools.getCycle(args);

      // Task tools
      case TaskMcpTools.TOOL_GET_TASKS -> taskTools.getTasks(args);
      case TaskMcpTools.TOOL_GET_TASK -> taskTools.getTask(args);
      case TaskMcpTools.TOOL_GET_BLOCKERS -> taskTools.getBlockers(args);
      case TaskMcpTools.TOOL_CREATE_TASK -> taskTools.createTask(args);
      case TaskMcpTools.TOOL_UPDATE_TASK_STATUS -> taskTools.updateTaskStatus(args);

      // Pitch tools
      case PitchMcpTools.TOOL_GET_PITCHES -> pitchTools.getPitches(args);
      case PitchMcpTools.TOOL_GET_PITCH -> pitchTools.getPitchDetail(args);
      case PitchMcpTools.TOOL_GET_BETTING_CANDIDATES -> pitchTools.getBettingCandidates(args);
      case PitchMcpTools.TOOL_CREATE_PITCH -> pitchTools.createPitch(args);
      case PitchMcpTools.TOOL_UPDATE_PITCH_STATUS -> pitchTools.updatePitchStatus(args);

      // Comment write tools — auth passed explicitly to avoid SecurityContextHolder on executor thread
      case CommentMcpTools.TOOL_ADD_COMMENT -> commentTools.addComment(args, auth);

      // Wise Architecture tools — auth passed for user scoping and history persistence
      case WiseArchitectureMcpTools.TOOL_LIST_ANALYSES -> wiseArchitectureTools.listAnalyses(args, auth);
      case WiseArchitectureMcpTools.TOOL_GET_FILES -> wiseArchitectureTools.getFiles(args);
      case WiseArchitectureMcpTools.TOOL_ANALYZE -> wiseArchitectureTools.analyze(args, auth);

      default -> throw new McpToolException("Unknown tool: " + name);
    };
  }

  // ── Tool registries ───────────────────────────────────────────────────────

  private List<Map<String, Object>> readTools() {
    return List.of(
        ProjectMcpTools.listProjectsDefinition(),
        ProjectMcpTools.getProjectDefinition(),
        CycleMcpTools.getCyclesDefinition(),
        CycleMcpTools.getCycleDefinition(),
        TaskMcpTools.getTasksDefinition(),
        TaskMcpTools.getTaskDefinition(),
        TaskMcpTools.getBlockersDefinition(),
        PitchMcpTools.getPitchesDefinition(),
        PitchMcpTools.getPitchDetailDefinition(),
        PitchMcpTools.getBettingCandidatesDefinition(),
        WiseArchitectureMcpTools.listAnalysesDefinition(),
        WiseArchitectureMcpTools.getFilesDefinition());
  }

  /**
   * Single source of truth for all write tool definitions.
   *
   * <p>Static so it can be used to initialise {@link #WRITE_TOOL_NAMES} before any instance
   * exists, keeping the security-gate set and the tool-list perfectly in sync.
   */
  private static List<Map<String, Object>> writeToolDefinitions() {
    return List.of(
        TaskMcpTools.createTaskDefinition(),
        TaskMcpTools.updateTaskStatusDefinition(),
        PitchMcpTools.createPitchDefinition(),
        PitchMcpTools.updatePitchStatusDefinition(),
        CommentMcpTools.addCommentDefinition(),
        WiseArchitectureMcpTools.analyzeDefinition());
  }

  /** Instance accessor used by {@link #handleToolsList} and {@link #toolCount()}. */
  private List<Map<String, Object>> writeTools() {
    return writeToolDefinitions();
  }

  private boolean isWriteTool(String name) {
    // O(1) lookup — set is built once from writeToolDefinitions() at class-load time
    return WRITE_TOOL_NAMES.contains(name);
  }

  /**
   * Returns the total number of tools registered in this dispatcher.
   * Used by the health endpoint to avoid a hardcoded constant.
   */
  public int toolCount() {
    return readTools().size() + writeTools().size();
  }

  // ── JSON-RPC helpers ──────────────────────────────────────────────────────

  private Map<String, Object> jsonRpcSuccess(Object id, Object result) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("result", result);
    return response;
  }

  private Map<String, Object> jsonRpcError(Object id, int code, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("error", Map.of("code", code, "message", message));
    return response;
  }

  private void trySendError(String sessionId, Object id, int code, String message) {
    if (id == null) {
      return; // notifications don't get error responses
    }
    try {
      sessionManager.send(sessionId, jsonRpcError(id, code, message));
    } catch (IOException | IllegalArgumentException ex) {
      log.warn("Could not send MCP error to session {}: {}", sessionId, ex.getMessage());
    }
  }

  // ── Auth helpers ──────────────────────────────────────────────────────────

  private boolean hasWriteScope(Authentication auth) {
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .anyMatch(a ->
            ("SCOPE_" + ApiKeyScope.WRITE.name()).equals(a.getAuthority())
                || ("SCOPE_" + ApiKeyScope.ADMIN.name()).equals(a.getAuthority()));
  }

  /** Signals a user-facing error in a tool call (returns JSON-RPC error -32602). */
  static class McpToolException extends RuntimeException {
    McpToolException(String message) {
      super(message);
    }
  }
}
