package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.dto.comment.CommentDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CommentService;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CommentMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CycleMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.PitchMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.ProjectMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WiseArchitectureMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureHistoryService;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unit tests for the MCP tool dispatcher — verifies JSON-RPC routing and tool definitions without
 * starting a full Spring context.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class McpToolDispatcherTest {

  @Mock private McpSessionManager sessionManager;
  @Mock private ProjectService projectService;
  @Mock private CycleService cycleService;
  @Mock private TaskService taskService;
  @Mock private PitchService pitchService;
  @Mock private CommentService commentService;
  @Mock private UserRepository userRepository;
  @Mock private Authentication auth;
  @Mock private WiseArchitectureService wiseArchitectureService;
  @Mock private WiseArchitectureHistoryService wiseArchHistoryService;

  private McpToolDispatcher dispatcher;
  private McpServerProperties properties;
  private static final String SESSION_ID = "test-session-123";

  @BeforeEach
  void setUp() {
    properties = new McpServerProperties();
    properties.setEnabled(true);
    properties.setWriteEnabled(false);
    properties.setServerName("shipflow-test");

    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules(); // for Java time types

    ProjectMcpTools projectTools = new ProjectMcpTools(projectService);
    CycleMcpTools cycleTools = new CycleMcpTools(cycleService);
    TaskMcpTools taskTools = new TaskMcpTools(taskService, userRepository);
    PitchMcpTools pitchTools = new PitchMcpTools(pitchService);
    CommentMcpTools commentTools = new CommentMcpTools(commentService, userRepository);
    WiseArchitectureMcpTools wiseArchTools = new WiseArchitectureMcpTools(wiseArchitectureService, wiseArchHistoryService, userRepository);

    dispatcher = new McpToolDispatcher(
        sessionManager, properties, mapper,
        projectTools, cycleTools, taskTools, pitchTools, commentTools, wiseArchTools);

    McpSession session = new McpSession(
        SESSION_ID,
        new SseEmitter(0L),
        auth,
        Instant.now());
    when(sessionManager.get(SESSION_ID)).thenReturn(Optional.of(session));
  }

  // ── initialize ────────────────────────────────────────────────────────────

  @Test
  void initialize_returnsServerCapabilities() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "initialize",
        "params", Map.of(
            "protocolVersion", McpToolDispatcher.PROTOCOL_VERSION,
            "clientInfo", Map.of("name", "test-client")),
        "id", 1);

    // Capture what gets sent back via SSE
    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    assertThat(captured).containsKey("result");
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("protocolVersion")).isEqualTo(McpToolDispatcher.PROTOCOL_VERSION);
    assertThat(result).containsKey("serverInfo");
    assertThat(result).containsKey("capabilities");
  }

  // ── tools/list ────────────────────────────────────────────────────────────

  @Test
  void toolsList_returnsReadToolsWhenWriteDisabled() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 2);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");

    // Should have 10 read tools (no write tools since writeEnabled=false)
    assertThat(tools).hasSizeGreaterThanOrEqualTo(10);

    // Verify tool names
    List<String> toolNames = tools.stream()
        .map(t -> (String) t.get("name"))
        .toList();
    assertThat(toolNames).contains(
        "list_projects", "get_project",
        "get_cycles", "get_cycle",
        "get_tasks", "get_task", "get_blockers",
        "get_pitches", "get_pitch_detail", "get_betting_candidates");

    // update_task_status must NOT be present (write disabled)
    assertThat(toolNames).doesNotContain("update_task_status");
  }

  @Test
  void toolsList_includesWriteToolWhenWriteEnabled() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 2);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
    List<String> toolNames = tools.stream().map(t -> (String) t.get("name")).toList();

    assertThat(toolNames).contains("update_task_status");
  }

  // ── tools/call ────────────────────────────────────────────────────────────

  @Test
  void toolsCall_listProjects_returnsProjects() throws Exception {
    ProjectDTO project = ProjectDTO.builder()
        .id(1L)
        .name("Mobile App")
        .projectKey("MOB")
        .projectType(ProjectType.SHAPE_UP)
        .isActive(true)
        .activeCycleCount(1)
        .build();
    when(projectService.findAccessibleProjects()).thenReturn(List.of(project));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "list_projects",
            "arguments", Map.of()),
        "id", 3);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat(content).hasSize(1);
    assertThat((String) content.get(0).get("text")).contains("Mobile App");
  }

  @Test
  void toolsCall_getPitchDetail_includesWireframeLinks() throws Exception {
    PitchDTO pitch = PitchDTO.builder()
        .id(42L)
        .title("Auth Revamp")
        .problemStatement("Login is too slow")
        .solution("Redesign with WebAuthn")
        .wireframeLinks("https://www.figma.com/design/AbCdEfGhIjKlMnOp/Auth-Revamp")
        .risks("Browser support for WebAuthn")
        .build();
    when(pitchService.getPitchById(42L)).thenReturn(pitch);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_pitch_detail",
            "arguments", Map.of("pitchId", 42)),
        "id", 5);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    String text = (String) content.get(0).get("text");
    assertThat(text).contains("Auth Revamp");
    assertThat(text).contains("figma.com"); // wireframeLinks included
    assertThat(text).contains("WebAuthn");   // solution field included
  }

  @Test
  void toolsCall_unknownTool_sendsErrorResponse() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "does_not_exist", "arguments", Map.of()),
        "id", 6);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    assertThat(captured).containsKey("error");
  }

  @Test
  void toolsCall_writeToolWhenWriteDisabled_sendsSecurityError() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_status",
            "arguments", Map.of("taskId", 1, "status", "DONE")),
        "id", 7);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    assertThat(captured).containsKey("error");
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) captured.get("error");
    assertThat((String) error.get("message")).contains("Write tools are disabled");
  }

  // ── Write tool tests ──────────────────────────────────────────────────────

  @Test
  void toolsCall_createTask_returnsCreatedTask() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    TaskDTO task = TaskDTO.builder().id(99L).title("New Task from MCP").build();
    when(taskService.createTask(org.mockito.ArgumentMatchers.any())).thenReturn(task);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "create_task",
            "arguments", Map.of("cycleId", 1, "title", "New Task from MCP")),
        "id", 10);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat((String) content.get(0).get("text")).contains("New Task from MCP");
  }

  @Test
  void toolsCall_createPitch_returnsCreatedPitch() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    PitchDTO pitch = PitchDTO.builder().id(55L).title("New Pitch from MCP").status(PitchStatus.IDEA).build();
    when(pitchService.createPitch(org.mockito.ArgumentMatchers.any())).thenReturn(pitch);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "create_pitch",
            "arguments", Map.of(
                "title", "New Pitch from MCP",
                "problemStatement", "Users can't find their tasks",
                "appetiteDays", 14)),
        "id", 11);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat((String) content.get(0).get("text")).contains("New Pitch from MCP");
  }

  @Test
  void toolsCall_updatePitchStatus_returnsPitch() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    PitchDTO pitch = PitchDTO.builder().id(42L).title("Auth Revamp").status(PitchStatus.SHAPED).build();
    when(pitchService.updateStatus(42L, PitchStatus.SHAPED)).thenReturn(pitch);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_pitch_status",
            "arguments", Map.of("pitchId", 42, "status", "SHAPED")),
        "id", 12);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat((String) content.get(0).get("text")).contains("SHAPED");
  }

  @Test
  void toolsCall_addComment_successWhenWriteEnabled() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
    org.mockito.Mockito.doReturn("mcpuser").when(auth).getName();

    com.github.farzadsedaghatbin.shipflow.entity.User mcpUser =
        com.github.farzadsedaghatbin.shipflow.entity.User.builder()
            .id(7L)
            .username("mcpuser")
            .build();
    when(userRepository.findByUsername("mcpuser")).thenReturn(Optional.of(mcpUser));

    CommentDTO comment = CommentDTO.builder()
        .id(101L)
        .content("Looks good to me!")
        .build();
    when(commentService.createComment(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(7L)))
        .thenReturn(comment);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "add_comment",
            "arguments", Map.of("entityType", "TASK", "entityId", 5, "content", "Looks good to me!")),
        "id", 15);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat((String) content.get(0).get("text")).contains("Looks good to me!");

    org.mockito.Mockito.verify(commentService)
        .createComment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(7L));
  }

  @Test
  void toolsCall_addComment_rejectsWhenWriteDisabled() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "add_comment",
            "arguments", Map.of("entityType", "TASK", "entityId", 1, "content", "Looks good")),
        "id", 13);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    assertThat(captured).containsKey("error");
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) captured.get("error");
    assertThat((String) error.get("message")).contains("Write tools are disabled");
  }

  @Test
  void toolsList_writeEnabled_includesAllPhase2Tools() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 14);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
    List<String> toolNames = tools.stream().map(t -> (String) t.get("name")).toList();

    assertThat(toolNames).contains(
        "create_task", "update_task_status",
        "create_pitch", "update_pitch_status",
        "add_comment");
  }

  // ── Tool definitions ──────────────────────────────────────────────────────

  @Test
  void pitchDefinition_mentionsFigmaInDescription() {
    Map<String, Object> def = PitchMcpTools.getPitchDetailDefinition();
    String description = (String) def.get("description");
    assertThat(description).containsIgnoringCase("figma");
    assertThat(description).containsIgnoringCase("wireframe");
  }

  @Test
  void allToolDefinitions_haveRequiredFields() {
    List<Map<String, Object>> all = List.of(
        ProjectMcpTools.listProjectsDefinition(),
        ProjectMcpTools.getProjectDefinition(),
        CycleMcpTools.getCyclesDefinition(),
        CycleMcpTools.getCycleDefinition(),
        TaskMcpTools.getTasksDefinition(),
        TaskMcpTools.getTaskDefinition(),
        TaskMcpTools.getBlockersDefinition(),
        TaskMcpTools.createTaskDefinition(),
        TaskMcpTools.updateTaskStatusDefinition(),
        PitchMcpTools.getPitchesDefinition(),
        PitchMcpTools.getPitchDetailDefinition(),
        PitchMcpTools.getBettingCandidatesDefinition(),
        PitchMcpTools.createPitchDefinition(),
        PitchMcpTools.updatePitchStatusDefinition(),
        CommentMcpTools.addCommentDefinition());

    for (Map<String, Object> def : all) {
      assertThat(def).as("Tool definition " + def.get("name"))
          .containsKeys("name", "description", "inputSchema");
      String desc = (String) def.get("description");
      assertThat(desc).isNotBlank();
    }
  }
}
