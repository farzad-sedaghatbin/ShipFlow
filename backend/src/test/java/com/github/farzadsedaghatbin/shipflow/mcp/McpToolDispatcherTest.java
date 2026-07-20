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
import com.github.farzadsedaghatbin.shipflow.service.BugReportService;
import com.github.farzadsedaghatbin.shipflow.service.CommentService;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import com.github.farzadsedaghatbin.shipflow.service.TestCaseService;
import com.github.farzadsedaghatbin.shipflow.service.TestRunService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import com.github.farzadsedaghatbin.shipflow.dto.HillChartPointDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.service.CycleService;
import com.github.farzadsedaghatbin.shipflow.service.HillChartService;
import com.github.farzadsedaghatbin.shipflow.service.RetroService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CommentMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.CycleMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.PitchMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.BugReportMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.IdentityMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.ProjectMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskContextMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TaskMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.TestCaseMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WiseArchitectureMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WorkContextMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.WorklogMcpTools;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureHistoryService;
import com.github.farzadsedaghatbin.shipflow.service.WorkLogService;
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
  @Mock private HillChartService hillChartService;
  @Mock private RetroService retroService;
  @Mock private WorkLogService workLogService;
  @Mock private TestCaseService testCaseService;
  @Mock private TestRunService testRunService;
  @Mock private BugReportService bugReportService;
  @Mock private com.github.farzadsedaghatbin.shipflow.service.DocumentService documentService;
  @Mock private com.github.farzadsedaghatbin.shipflow.service.mcp.McpUsageReportService usageReportService;

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
    WorkContextMcpTools workContextTools = new WorkContextMcpTools(pitchService, cycleService, taskService, hillChartService, retroService);
    TaskContextMcpTools taskContextTools = new TaskContextMcpTools(taskService, pitchService, cycleService, testCaseService, bugReportService);
    WorklogMcpTools worklogTools = new WorklogMcpTools(workLogService, userRepository);
    IdentityMcpTools identityTools = new IdentityMcpTools(userRepository);
    TestCaseMcpTools testCaseTools = new TestCaseMcpTools(testCaseService, testRunService, userRepository);
    BugReportMcpTools bugReportTools = new BugReportMcpTools(bugReportService, documentService, userRepository);

    dispatcher = new McpToolDispatcher(
        sessionManager, properties, mapper,
        projectTools, cycleTools, taskTools, pitchTools, commentTools, wiseArchTools,
        workContextTools, taskContextTools, worklogTools,
        identityTools, testCaseTools, bugReportTools);
    dispatcher.setUsageReportService(usageReportService);

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

    // Should have at least 11 read tools (no write tools since writeEnabled=false)
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
  void toolsCall_bindsSessionAuthToSecurityContextDuringToolExecution() throws Exception {
    // Read tools (e.g. ProjectService.getCurrentUser) read SecurityContextHolder, but tools run
    // on a virtual executor thread where McpAuthFilter's context is not visible. Verify the
    // dispatcher binds the session's Authentication to the thread for the duration of the call.
    org.mockito.Mockito.doReturn("mcpuser").when(auth).getName();

    var seenAuthName = new java.util.concurrent.atomic.AtomicReference<String>();
    when(projectService.findAccessibleProjects()).thenAnswer(inv -> {
      var ctxAuth = org.springframework.security.core.context.SecurityContextHolder
          .getContext().getAuthentication();
      seenAuthName.set(ctxAuth != null ? ctxAuth.getName() : null);
      return List.of();
    });

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "list_projects", "arguments", Map.of()),
        "id", 30);

    org.mockito.Mockito.doAnswer(inv -> null).when(sessionManager)
        .send(org.mockito.ArgumentMatchers.eq(SESSION_ID), org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    // The service saw the authenticated principal while executing...
    assertThat(seenAuthName.get()).isEqualTo("mcpuser");
    // ...and the context is cleared afterwards so it doesn't leak to the next task on this thread.
    assertThat(org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication()).isNull();
  }

  @Test
  void toolsCall_getPitchesByProject_skipsPitchesWithNullProjectId() throws Exception {
    // Idea-stage pitches have a null projectId. The project filter must not NPE on them
    // (regression: `projectId == p.getProjectId()` auto-unboxed null -> NullPointerException).
    PitchDTO inProject = PitchDTO.builder().id(1L).title("In Project").projectId(5L).build();
    PitchDTO orphan = PitchDTO.builder().id(2L).title("Idea Pitch").projectId(null).build();
    when(pitchService.getAccessiblePitches()).thenReturn(List.of(inProject, orphan));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_pitches", "arguments", Map.of("projectId", 5)),
        "id", 31);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    // No error, and only the project-5 pitch comes back (the null-projectId one is filtered out).
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) captured.get("result");
    assertThat(result).as("should not have errored on null projectId").isNotNull();
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    String text = (String) content.get(0).get("text");
    assertThat(text).contains("In Project");
    assertThat(text).doesNotContain("Idea Pitch");
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
  void toolsCall_missingRequiredArgument_returnsInvalidParamsNotInternalError() throws Exception {
    // A bad LLM call (missing a required arg) is a client error, not a server crash. It should come
    // back as JSON-RPC -32602 (Invalid params), not -32603 (Internal error).
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_pitch_detail", "arguments", Map.of()), // pitchId omitted
        "id", 8);

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
    assertThat(error.get("code")).isEqualTo(-32602);
    assertThat((String) error.get("message")).contains("pitchId");
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
        "create_task", "update_task", "update_task_status",
        "create_pitch", "update_pitch", "update_pitch_status",
        "add_comment", "log_work");
  }

  // ── log_work ──────────────────────────────────────────────────────────────

  @Test
  void toolsCall_logWork_logsTimeAndReturnsWorklog() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
    org.mockito.Mockito.doReturn("mcpuser").when(auth).getName();

    com.github.farzadsedaghatbin.shipflow.entity.Person person =
        com.github.farzadsedaghatbin.shipflow.entity.Person.builder()
            .id(3L)
            .name("MCP User")
            .build();
    com.github.farzadsedaghatbin.shipflow.entity.User mcpUser =
        com.github.farzadsedaghatbin.shipflow.entity.User.builder()
            .id(7L)
            .username("mcpuser")
            .person(person)
            .build();
    when(userRepository.findByUsernameWithPerson("mcpuser")).thenReturn(Optional.of(mcpUser));

    com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO worklog =
        com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO.builder()
            .id(201L)
            .taskId(59L)
            .taskTitle("POST /shorten")
            .personId(3L)
            .personName("MCP User")
            .hoursSpent(new java.math.BigDecimal("1.5"))
            .date(java.time.LocalDate.of(2026, 6, 1))
            .build();
    when(workLogService.createWorkLog(org.mockito.ArgumentMatchers.any())).thenReturn(worklog);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "log_work",
            "arguments", Map.of("taskId", 59, "hoursSpent", 1.5, "date", "2026-06-01",
                "note", "Implemented POST /shorten endpoint")),
        "id", 40);

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
    assertThat(text).contains("1.5");
    assertThat(text).contains("POST /shorten");
  }

  @Test
  void toolsCall_logWork_rejectsWhenWriteDisabled() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "log_work",
            "arguments", Map.of("taskId", 1, "hoursSpent", 1.0)),
        "id", 41);

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

  // ── Tool definitions ──────────────────────────────────────────────────────

  // ── get_work_context ──────────────────────────────────────────────────────

  @Test
  void toolsCall_getWorkContextByPitch_returnsFullGraph() throws Exception {
    PitchDTO pitch = PitchDTO.builder()
        .id(10L)
        .title("Search Revamp")
        .cycleId(5L)
        .cycleName("Cycle 1")
        .problemStatement("Search is slow")
        .solution("Add Elasticsearch")
        .build();
    when(pitchService.getPitchById(10L)).thenReturn(pitch);

    CycleDTO cycle = CycleDTO.builder()
        .id(5L)
        .name("Cycle 1")
        .projectId(1L)
        .projectName("Backend")
        .build();
    when(cycleService.getCycleById(5L)).thenReturn(cycle);

    TaskDTO task = TaskDTO.builder()
        .id(20L)
        .title("Index articles")
        .build();
    when(taskService.getTasksByPitchId(10L)).thenReturn(List.of(task));
    when(hillChartService.getHillChartPointsByPitch(10L)).thenReturn(List.of(
        HillChartPointDTO.builder()
            .id(1L)
            .scope("Indexing")
            .position(40)
            .pitchId(10L)
            .pitchTitle("Search Revamp")
            .build()));
    when(retroService.getRetrosByCycle(5L)).thenReturn(List.of(
        RetroDTO.builder()
            .id(3L)
            .title("Cycle 1 Retro")
            .status(RetroStatus.CLOSED)
            .itemCount(5)
            .cycleId(5L)
            .cycleName("Cycle 1")
            .build()));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_work_context",
            "arguments", Map.of("pitchId", 10)),
        "id", 20);

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
    assertThat(text).contains("Search Revamp");
    assertThat(text).contains("Cycle 1");
    assertThat(text).contains("Index articles");
    assertThat(text).contains("Indexing");     // hill chart scope
    assertThat(text).contains("Cycle 1 Retro"); // retro summary
  }

  @Test
  void toolsCall_getWorkContextByCycle_returnsAllPitchesAndTasks() throws Exception {
    CycleDTO cycle = CycleDTO.builder()
        .id(7L)
        .name("Cycle 2")
        .projectId(1L)
        .projectName("Mobile")
        .build();
    when(cycleService.getCycleById(7L)).thenReturn(cycle);

    PitchDTO pitch = PitchDTO.builder()
        .id(30L)
        .title("Onboarding Flow")
        .cycleId(7L)
        .build();
    when(pitchService.getPitchesByCycleId(7L)).thenReturn(List.of(pitch));

    TaskDTO blockedTask = TaskDTO.builder()
        .id(50L)
        .title("Design screens")
        .isBlocked(true)
        .build();
    when(taskService.getTasksByCycleId(7L)).thenReturn(List.of(blockedTask));
    when(hillChartService.getHillChartPointsByCycle(7L)).thenReturn(List.of());
    when(retroService.getRetrosByCycle(7L)).thenReturn(List.of());

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_work_context",
            "arguments", Map.of("cycleId", 7)),
        "id", 21);

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
    assertThat(text).contains("Cycle 2");
    assertThat(text).contains("Onboarding Flow");
    assertThat(text).contains("Design screens");
    assertThat(text).contains("blockers"); // blocked task present in blockers list
  }

  @Test
  void toolsCall_getWorkContextWithoutArgs_sendsError() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_work_context",
            "arguments", Map.of()),
        "id", 22);

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
    String message = (String) error.get("message");
    assertThat(message).containsIgnoringCase("pitchId");
    assertThat(message).containsIgnoringCase("taskId");
  }

  @Test
  void workContextDefinition_hasRequiredFields() {
    Map<String, Object> def = WorkContextMcpTools.getWorkContextDefinition();
    assertThat(def).containsKeys("name", "description", "inputSchema");
    assertThat((String) def.get("description")).containsIgnoringCase("hill");
    assertThat((String) def.get("description")).containsIgnoringCase("retro");
    assertThat((String) def.get("description")).containsIgnoringCase("blocker");
  }

  @Test
  void toolsList_includesGetWorkContext() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 23);

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
    assertThat(toolNames).contains("get_work_context");
  }

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
        CommentMcpTools.addCommentDefinition(),
        WorkContextMcpTools.getWorkContextDefinition(),
        TaskContextMcpTools.getTaskContextDefinition(),
        WorklogMcpTools.logWorkDefinition());

    for (Map<String, Object> def : all) {
      assertThat(def).as("Tool definition " + def.get("name"))
          .containsKeys("name", "description", "inputSchema");
      String desc = (String) def.get("description");
      assertThat(desc).isNotBlank();
    }
  }

  // ── get_task_context ──────────────────────────────────────────────────────

  @Test
  void toolsCall_getTaskContext_returnsTaskPitchSiblingsAndHints() throws Exception {
    TaskDTO focal = TaskDTO.builder()
        .id(8L)
        .title("Wire click analytics")
        .description("Send pageviews to analytics")
        .pitchId(10L)
        .pitchTitle("Click analytics")
        .cycleId(5L)
        .cycleName("Cycle 1")
        .isBlocked(true)
        .blockedByCount(2)
        .build();
    when(taskService.getTaskById(8L)).thenReturn(focal);

    PitchDTO pitch = PitchDTO.builder()
        .id(10L)
        .title("Click analytics")
        .cycleId(5L)
        .cycleName("Cycle 1")
        .problemStatement("We don't know what users click")
        .solution("Add a click tracking layer")
        .wireframeLinks("https://figma.com/file/abc123")
        .build();
    when(pitchService.getPitchById(10L)).thenReturn(pitch);

    CycleDTO cycle = CycleDTO.builder()
        .id(5L)
        .name("Cycle 1")
        .projectId(1L)
        .projectName("Web")
        .build();
    when(cycleService.getCycleById(5L)).thenReturn(cycle);

    TaskDTO sibling = TaskDTO.builder()
        .id(9L)
        .title("Add tracking endpoint")
        .pitchId(10L)
        .build();
    when(taskService.getTasksByPitchId(10L)).thenReturn(List.of(focal, sibling));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_task_context",
            "arguments", Map.of("taskId", 8)),
        "id", 30);

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

    // Focal task surfaced as `task`, not duplicated into siblings
    assertThat(text).contains("Wire click analytics");
    assertThat(text).contains("Add tracking endpoint"); // sibling
    // Pitch context (Shape Up + Figma URL)
    assertThat(text).contains("Click analytics");
    assertThat(text).contains("Add a click tracking layer");
    assertThat(text).contains("figma.com/file/abc123");
    // Cycle metadata
    assertThat(text).contains("Cycle 1");
    // Hints — Figma + blocked
    assertThat(text).containsIgnoringCase("Figma MCP");
    assertThat(text).containsIgnoringCase("BLOCKED");
  }

  @Test
  void toolsCall_getTaskContext_missingTaskId_sendsError() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_task_context",
            "arguments", Map.of()),
        "id", 31);

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
    assertThat((String) error.get("message")).containsIgnoringCase("taskId");
  }

  @Test
  void toolsCall_getTaskContext_taskWithoutPitch_fallsBackToCycleSiblings() throws Exception {
    TaskDTO focal = TaskDTO.builder()
        .id(99L)
        .title("Kanban-only task")
        .cycleId(5L)
        .cycleName("Backlog")
        // no pitchId — Kanban project
        .build();
    when(taskService.getTaskById(99L)).thenReturn(focal);

    CycleDTO cycle = CycleDTO.builder().id(5L).name("Backlog").projectId(1L).build();
    when(cycleService.getCycleById(5L)).thenReturn(cycle);

    TaskDTO sibling = TaskDTO.builder().id(100L).title("Another kanban task").cycleId(5L).build();
    when(taskService.getTasksByCycleId(5L)).thenReturn(List.of(focal, sibling));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_task_context",
            "arguments", Map.of("taskId", 99)),
        "id", 32);

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

    assertThat(text).contains("Another kanban task");
    assertThat(text).containsIgnoringCase("not linked to a pitch");
    // Pitch service must not be called when there's no pitchId
    org.mockito.Mockito.verify(pitchService, org.mockito.Mockito.never())
        .getPitchById(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void toolsCall_getWorkContext_acceptsTaskId() throws Exception {
    TaskDTO task = TaskDTO.builder()
        .id(8L)
        .title("Wire click analytics")
        .pitchId(10L)
        .cycleId(5L)
        .build();
    when(taskService.getTaskById(8L)).thenReturn(task);

    PitchDTO pitch = PitchDTO.builder().id(10L).title("Click analytics").cycleId(5L).build();
    when(pitchService.getPitchById(10L)).thenReturn(pitch);

    CycleDTO cycle = CycleDTO.builder().id(5L).name("Cycle 1").projectId(1L).build();
    when(cycleService.getCycleById(5L)).thenReturn(cycle);

    when(taskService.getTasksByPitchId(10L)).thenReturn(List.of(task));
    when(hillChartService.getHillChartPointsByPitch(10L)).thenReturn(List.of());
    when(retroService.getRetrosByCycle(5L)).thenReturn(List.of());

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_work_context",
            "arguments", Map.of("taskId", 8)),
        "id", 33);

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
    assertThat(text).contains("Click analytics");
  }

  @Test
  void taskContextDefinition_hasRequiredFields() {
    Map<String, Object> def = TaskContextMcpTools.getTaskContextDefinition();
    assertThat(def).containsKeys("name", "description", "inputSchema");
    assertThat((String) def.get("name")).isEqualTo("get_task_context");
    assertThat((String) def.get("description")).containsIgnoringCase("figma");
    assertThat((String) def.get("description")).containsIgnoringCase("hint");

    @SuppressWarnings("unchecked")
    Map<String, Object> schema = (Map<String, Object>) def.get("inputSchema");
    @SuppressWarnings("unchecked")
    List<String> required = (List<String>) schema.get("required");
    assertThat(required).contains("taskId");
  }

  @Test
  void toolsList_includesGetTaskContext() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 34);

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
    assertThat(toolNames).contains("get_task_context");
  }

  // ── whoami ────────────────────────────────────────────────────────────────

  @Test
  void toolsCall_whoami_returnsAuthenticatedIdentity() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.Person person =
        new com.github.farzadsedaghatbin.shipflow.entity.Person();
    person.setId(42L);
    person.setName("Farzad Sedaghatbin");

    com.github.farzadsedaghatbin.shipflow.entity.User user =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    user.setId(7L);
    user.setUsername("farzad");
    user.setEmail("farzad@example.com");
    user.setRole(com.github.farzadsedaghatbin.shipflow.entity.UserRole.ADMIN);
    user.setPerson(person);

    when(auth.getName()).thenReturn("farzad");
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(user));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "whoami", "arguments", Map.of()),
        "id", 40);

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
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    String text = (String) content.get(0).get("text");
    assertThat(text).contains("farzad");
    assertThat(text).contains("ADMIN");
    assertThat(text).contains("42");          // personId
    assertThat(text).contains("Farzad Sedaghatbin");
  }

  // ── get_tasks filters ────────────────────────────────────────────────────

  @Test
  void toolsCall_getTasks_assigneeIdFilter_callsRepoByPerson() throws Exception {
    TaskDTO mine = TaskDTO.builder().id(1L).title("My task").assigneeId(42L).build();
    when(taskService.getTasksByPersonId(42L)).thenReturn(List.of(mine));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_tasks",
            "arguments", Map.of("assigneeId", 42)),
        "id", 41);

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
    assertThat((String) content.get(0).get("text")).contains("My task");
  }

  @Test
  void toolsCall_getTasks_mineWithCycle_filtersToCallersAssignedTasks() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.Person person =
        new com.github.farzadsedaghatbin.shipflow.entity.Person();
    person.setId(42L);
    com.github.farzadsedaghatbin.shipflow.entity.User user =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    user.setUsername("farzad");
    user.setPerson(person);
    when(auth.getName()).thenReturn("farzad");
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(user));

    TaskDTO mine = TaskDTO.builder().id(1L).title("Wire analytics").assigneeId(42L).build();
    TaskDTO other = TaskDTO.builder().id(2L).title("Someone else's task").assigneeId(99L).build();
    when(taskService.getTasksByCycleId(5L)).thenReturn(List.of(mine, other));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_tasks",
            "arguments", Map.of("cycleId", 5, "mine", true)),
        "id", 42);

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
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    String text = (String) content.get(0).get("text");
    assertThat(text).contains("Wire analytics");
    assertThat(text).doesNotContain("Someone else's task");
  }

  @Test
  void toolsCall_getTasks_noScope_sendsError() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_tasks", "arguments", Map.of()),
        "id", 43);

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
    assertThat((String) error.get("message")).containsIgnoringCase("scope");
  }

  // ── create_task w/ parentTaskId (subtask) ────────────────────────────────

  @Test
  void toolsCall_createTask_withParentTaskId_passesParentToService() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));

    TaskDTO created = TaskDTO.builder().id(99L).title("Subtask").parentTaskId(8L).build();
    when(taskService.createTask(org.mockito.ArgumentMatchers.argThat(req ->
        req != null && req.getParentTaskId() != null && req.getParentTaskId() == 8L
            && "Subtask".equals(req.getTitle()))))
        .thenReturn(created);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "create_task",
            "arguments", Map.of(
                "cycleId", 5,
                "title", "Subtask",
                "parentTaskId", 8)),
        "id", 44);

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
    assertThat((String) content.get(0).get("text")).contains("Subtask");
  }

  // ── QA: test cases + runs ────────────────────────────────────────────────

  @Test
  void toolsCall_getTestCases_byTaskId_returnsAcceptanceCriteria() throws Exception {
    com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO tc =
        com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO.builder()
            .id(1L).testCaseKey("TC-1").title("Click is tracked")
            .preconditions("User is logged in")
            .steps("Click the button")
            .expectedResult("Event sent to analytics")
            .taskId(8L).taskTitle("Wire click analytics")
            .build();
    when(testCaseService.getTestCasesByTask(8L)).thenReturn(List.of(tc));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "get_test_cases",
            "arguments", Map.of("taskId", 8)),
        "id", 50);

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
    assertThat(text).contains("Click is tracked");
    assertThat(text).contains("Event sent to analytics");
  }

  @Test
  void toolsCall_recordTestRun_invokesService() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));
    when(auth.getName()).thenReturn("farzad");

    com.github.farzadsedaghatbin.shipflow.entity.User user =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    user.setId(7L);
    user.setUsername("farzad");
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(user));

    com.github.farzadsedaghatbin.shipflow.dto.qa.TestRunDTO run =
        com.github.farzadsedaghatbin.shipflow.dto.qa.TestRunDTO.builder()
            .id(100L)
            .testCaseId(1L)
            .status(com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus.PASSED)
            .notes("Looks good")
            .build();
    when(testRunService.createTestRun(
        org.mockito.ArgumentMatchers.argThat(req ->
            req != null && req.getTestCaseId() != null && req.getTestCaseId() == 1L
                && com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus.PASSED.equals(req.getStatus())),
        org.mockito.ArgumentMatchers.eq(7L)))
        .thenReturn(run);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "record_test_run",
            "arguments", Map.of("testCaseId", 1, "status", "PASSED", "notes", "Looks good")),
        "id", 51);

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
    assertThat(text).contains("PASSED");
    assertThat(text).contains("Looks good");
  }

  // ── Bug reports ──────────────────────────────────────────────────────────

  @Test
  void toolsCall_getBugReports_byTaskId_returnsBugs() throws Exception {
    com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO bug =
        com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO.builder()
            .id(1L).bugKey("BUG-1").title("Click handler silently fails")
            .stepsToReproduce("Click button while offline")
            .actualBehavior("Nothing happens")
            .expectedBehavior("Event queued for retry")
            .severity(com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity.MAJOR)
            .status(com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus.OPEN)
            .taskId(8L).taskTitle("Wire click analytics")
            .build();
    when(bugReportService.getBugReportsByTask(8L)).thenReturn(List.of(bug));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_bug_reports", "arguments", Map.of("taskId", 8)),
        "id", 60);

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
    assertThat(text).contains("Click handler silently fails");
    assertThat(text).contains("MAJOR");
  }

  @Test
  void toolsCall_downloadBugAttachment_returnsImageContentBlock() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument doc =
        new com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument();
    doc.setId(55L);
    doc.setEntityType("BUG_REPORT");
    doc.setOriginalFileName("final-design.png");
    doc.setFileType("png");
    doc.setFileSize(1234L);
    when(documentService.getDocumentById(55L)).thenReturn(doc);
    when(documentService.getContentType("png")).thenReturn("image/png");
    byte[] pngBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
    when(documentService.getDocumentBytes(55L)).thenReturn(pngBytes);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "download_bug_attachment", "arguments", Map.of("attachmentId", 55)),
        "id", 63);

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
    // text note + image block
    Map<String, Object> imageBlock = content.stream()
        .filter(c -> "image".equals(c.get("type"))).findFirst().orElseThrow();
    assertThat(imageBlock.get("mimeType")).isEqualTo("image/png");
    assertThat(imageBlock.get("data"))
        .isEqualTo(java.util.Base64.getEncoder().encodeToString(pngBytes));
  }

  @Test
  void toolsCall_downloadBugAttachment_rejectsNonImage() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument doc =
        new com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument();
    doc.setId(56L);
    doc.setEntityType("BUG_REPORT");
    doc.setOriginalFileName("trace.pdf");
    doc.setFileType("pdf");
    doc.setFileSize(999L);
    when(documentService.getDocumentById(56L)).thenReturn(doc);
    when(documentService.getContentType("pdf")).thenReturn("application/pdf");

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "download_bug_attachment", "arguments", Map.of("attachmentId", 56)),
        "id", 64);

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
    assertThat((String) error.get("message")).contains("not an image");
  }

  @Test
  void toolsCall_getBugReport_byBugKey_resolvesViaKey() throws Exception {
    com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO bug =
        com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO.builder()
            .id(999L).bugKey("BUG-125").title("Convert money confirmation loops")
            .severity(com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity.MAJOR)
            .status(com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus.OPEN)
            .build();
    when(bugReportService.getBugReportByKey("BUG-125")).thenReturn(bug);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_bug_report", "arguments", Map.of("bugKey", "BUG-125")),
        "id", 62);

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
    assertThat(text).contains("Convert money confirmation loops");
    assertThat(text).contains("BUG-125");
    org.mockito.Mockito.verify(bugReportService).getBugReportByKey("BUG-125");
    org.mockito.Mockito.verify(bugReportService, org.mockito.Mockito.never())
        .getBugReportById(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void toolsCall_updateBugStatus_updatesAndStampsResolution() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));
    when(auth.getName()).thenReturn("farzad");

    com.github.farzadsedaghatbin.shipflow.entity.User user =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    user.setId(7L); user.setUsername("farzad");
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(user));

    com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO updated =
        com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO.builder()
            .id(1L).bugKey("BUG-1").title("Click handler fix")
            .status(com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus.RESOLVED)
            .resolution("Queued events via offline buffer")
            .build();
    when(bugReportService.updateBugReport(
        org.mockito.ArgumentMatchers.eq(1L),
        org.mockito.ArgumentMatchers.argThat(req ->
            req != null
                && com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus.RESOLVED.equals(req.getStatus())
                && "Queued events via offline buffer".equals(req.getResolution())),
        org.mockito.ArgumentMatchers.eq(7L)))
        .thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_bug_status",
            "arguments", Map.of(
                "bugReportId", 1,
                "status", "RESOLVED",
                "resolution", "Queued events via offline buffer")),
        "id", 61);

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
    assertThat(text).contains("RESOLVED");
    assertThat(text).contains("offline buffer");
  }

  // ── Context aggregator surfaces test + bug counts ────────────────────────

  @Test
  void toolsCall_getTaskContext_includesTestAndBugCounts() throws Exception {
    TaskDTO task = TaskDTO.builder()
        .id(8L).title("Wire click analytics").pitchId(10L).cycleId(5L).build();
    when(taskService.getTaskById(8L)).thenReturn(task);

    com.github.farzadsedaghatbin.shipflow.dto.PitchDTO pitch =
        com.github.farzadsedaghatbin.shipflow.dto.PitchDTO.builder()
            .id(10L).title("Click analytics").cycleId(5L).build();
    when(pitchService.getPitchById(10L)).thenReturn(pitch);

    CycleDTO cycle = CycleDTO.builder().id(5L).name("Cycle 1").build();
    when(cycleService.getCycleById(5L)).thenReturn(cycle);
    when(taskService.getTasksByPitchId(10L)).thenReturn(List.of(task));

    when(testCaseService.countTestCasesByTask(8L)).thenReturn(3L);
    when(bugReportService.countBugReportsByTask(8L)).thenReturn(1L);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_task_context", "arguments", Map.of("taskId", 8)),
        "id", 70);

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
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    String text = (String) content.get(0).get("text");
    assertThat(text).contains("testCaseCount");
    assertThat(text).contains("bugReportCount");
    // Hints surface the counts so the agent knows to call get_test_cases / get_bug_reports
    assertThat(text).containsIgnoringCase("test case(s) attached");
    assertThat(text).containsIgnoringCase("bug report(s) linked");
  }

  // ── update_task_assignee ─────────────────────────────────────────────────

  @Test
  void toolsCall_updateTaskAssignee_byUsername_resolvesPersonAndUpdates() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));

    com.github.farzadsedaghatbin.shipflow.entity.Person person =
        new com.github.farzadsedaghatbin.shipflow.entity.Person();
    person.setId(42L);
    person.setName("Farzad Sedaghatbin");
    com.github.farzadsedaghatbin.shipflow.entity.User assignee =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    assignee.setUsername("farzad");
    assignee.setPerson(person);
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(assignee));

    TaskDTO updated = TaskDTO.builder().id(8L).title("Wire click analytics")
        .assigneeId(42L).assigneeName("Farzad Sedaghatbin").build();
    when(taskService.updateTaskAssignee(8L, 42L)).thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_assignee",
            "arguments", Map.of("taskId", 8, "assigneeUsername", "farzad")),
        "id", 80);

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
    assertThat(text).contains("Farzad Sedaghatbin");
    org.mockito.Mockito.verify(taskService).updateTaskAssignee(8L, 42L);
  }

  @Test
  void toolsCall_updateTaskAssignee_mine_resolvesToCaller() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));
    when(auth.getName()).thenReturn("farzad");

    com.github.farzadsedaghatbin.shipflow.entity.Person person =
        new com.github.farzadsedaghatbin.shipflow.entity.Person();
    person.setId(42L);
    com.github.farzadsedaghatbin.shipflow.entity.User caller =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    caller.setUsername("farzad");
    caller.setPerson(person);
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(caller));

    TaskDTO updated = TaskDTO.builder().id(8L).title("T").assigneeId(42L).build();
    when(taskService.updateTaskAssignee(8L, 42L)).thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_assignee",
            "arguments", Map.of("taskId", 8, "mine", true)),
        "id", 81);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    org.mockito.Mockito.verify(taskService).updateTaskAssignee(8L, 42L);
  }

  @Test
  void toolsCall_updateTaskAssignee_unassign_clearsAssignee() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));

    TaskDTO updated = TaskDTO.builder().id(8L).title("T").build();
    when(taskService.updateTaskAssignee(8L, null)).thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_assignee",
            "arguments", Map.of("taskId", 8, "unassign", true)),
        "id", 82);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    org.mockito.Mockito.verify(taskService).updateTaskAssignee(8L, null);
  }

  @Test
  void toolsCall_updateTaskAssignee_noOption_sendsError() throws Exception {
    properties.setWriteEnabled(true);
    when(auth.getAuthorities()).thenReturn(
        (java.util.Collection) java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE")));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_assignee",
            "arguments", Map.of("taskId", 8)),
        "id", 83);

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
    assertThat((String) error.get("message")).containsIgnoringCase("assigneeUsername");
  }

  // ── tools/list now includes all new tools ────────────────────────────────

  @Test
  void toolsList_includesNewTools() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/list",
        "id", 71);

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
        "whoami",
        "get_test_cases", "get_test_case", "get_test_runs",
        "get_bug_reports", "get_bug_report");
    // Write tools must NOT be present (writeEnabled=false at setUp; test methods that need them
    // flip the property locally and don't affect this one)
    assertThat(toolNames).doesNotContain("record_test_run");
    assertThat(toolNames).doesNotContain("update_bug_status");
  }

  // ── update_task ───────────────────────────────────────────────────────────

  @Test
  void toolsCall_updateTask_patchesFieldsAndReturnsTask() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    TaskDTO current = TaskDTO.builder()
        .id(77L)
        .title("Old Title")
        .description("Old description")
        .cycleId(1L)
        .build();
    when(taskService.getTaskById(77L)).thenReturn(current);

    TaskDTO updated = TaskDTO.builder().id(77L).title("New Title").build();
    when(taskService.updateTask(org.mockito.ArgumentMatchers.eq(77L),
        org.mockito.ArgumentMatchers.any())).thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task",
            "arguments", Map.of("taskId", 77, "title", "New Title")),
        "id", 200);

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
    assertThat((String) content.get(0).get("text")).contains("New Title");
  }

  @Test
  void toolsCall_updateTask_missingTaskId_returnsInvalidParams() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "update_task", "arguments", Map.of("title", "Oops")),
        "id", 201);

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
    assertThat(error.get("code")).isEqualTo(-32602);
  }

  @Test
  void toolsCall_updateTask_blockedWhenWriteDisabled() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "update_task",
            "arguments", Map.of("taskId", 1, "title", "Should not work")),
        "id", 202);

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

  // ── update_pitch ──────────────────────────────────────────────────────────

  @Test
  void toolsCall_updatePitch_patchesShapeUpFieldsAndReturnsPitch() throws Exception {
    properties.setWriteEnabled(true);
    java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities =
        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_WRITE"));
    org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();

    PitchDTO current = PitchDTO.builder()
        .id(42L)
        .title("Auth Revamp")
        .status(PitchStatus.DRAFT)
        .problemStatement("Login is slow")
        .build();
    when(pitchService.getPitchById(42L)).thenReturn(current);

    PitchDTO updated = PitchDTO.builder()
        .id(42L)
        .title("Auth Revamp")
        .status(PitchStatus.DRAFT)
        .solution("Use WebAuthn for passwordless login")
        .wireframeLinks("https://www.figma.com/design/AbCd/Auth")
        .build();
    when(pitchService.updatePitch(org.mockito.ArgumentMatchers.eq(42L),
        org.mockito.ArgumentMatchers.any())).thenReturn(updated);

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_pitch",
            "arguments", Map.of(
                "pitchId", 42,
                "solution", "Use WebAuthn for passwordless login",
                "wireframeLinks", "https://www.figma.com/design/AbCd/Auth")),
        "id", 210);

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
    assertThat(text).contains("WebAuthn");
    assertThat(text).contains("figma.com");
  }

  @Test
  void toolsCall_updatePitch_blockedWhenWriteDisabled() throws Exception {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "update_pitch",
            "arguments", Map.of("pitchId", 1, "title", "Should not work")),
        "id", 211);

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

  // ── process() — used directly by the Streamable HTTP transport, not just via dispatch()/SSE ──

  @Test
  void process_initialize_returnsSuccessResponseDirectlyWithoutSendingToSession() throws Exception {
    ProjectDTO project = ProjectDTO.builder()
        .id(1L).name("Mobile App").projectKey("MOB")
        .projectType(ProjectType.SHAPE_UP).isActive(true).activeCycleCount(1).build();
    when(projectService.findAccessibleProjects()).thenReturn(List.of(project));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "initialize",
        "params", Map.of(
            "protocolVersion", McpToolDispatcher.PROTOCOL_VERSION,
            "clientInfo", Map.of("name", "test-client")),
        "id", 100);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("result");
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) response.get("result");
    assertThat(result.get("protocolVersion")).isEqualTo(McpToolDispatcher.PROTOCOL_VERSION);
    assertThat(result).containsKey("serverInfo");

    // process() must be a pure computation — it never touches the session manager's send() path.
    org.mockito.Mockito.verify(sessionManager, org.mockito.Mockito.never())
        .send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void process_toolsCall_returnsSuccessResponseMap() throws Exception {
    ProjectDTO project = ProjectDTO.builder()
        .id(1L).name("Mobile App").projectKey("MOB")
        .projectType(ProjectType.SHAPE_UP).isActive(true).activeCycleCount(1).build();
    when(projectService.findAccessibleProjects()).thenReturn(List.of(project));

    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "list_projects", "arguments", Map.of()),
        "id", 101);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("result");
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) response.get("result");
    assertThat(result.get("isError")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
    assertThat((String) content.get(0).get("text")).contains("Mobile App");
  }

  @Test
  void process_invalidParams_returnsErrorMapNotThrown() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "get_pitch_detail", "arguments", Map.of()), // pitchId omitted
        "id", 102);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("error");
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) response.get("error");
    assertThat(error.get("code")).isEqualTo(-32602);
    assertThat((String) error.get("message")).contains("pitchId");
  }

  @Test
  void process_securityError_returnsErrorMapNotThrown() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of(
            "name", "update_task_status",
            "arguments", Map.of("taskId", 1, "status", "DONE")),
        "id", 103);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("error");
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) response.get("error");
    assertThat((String) error.get("message")).contains("Write tools are disabled");
  }

  @Test
  void process_unknownMethod_returnsMethodNotFoundError() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "does/not/exist",
        "id", 104);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("error");
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) response.get("error");
    assertThat(error.get("code")).isEqualTo(-32601);
  }

  @Test
  void process_unknownTool_returnsErrorMap() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "tools/call",
        "params", Map.of("name", "does_not_exist", "arguments", Map.of()),
        "id", 105);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("error");
  }

  @Test
  void process_notification_returnsNull() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "notifications/initialized");
    // No "id" — this is a notification, not a request.

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).isNull();
  }

  @Test
  void process_unknownMethodWithNoId_returnsNull() {
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0",
        "method", "some/unknown/notification");
    // No "id" — per JSON-RPC, no reply expected even for an unrecognised method.

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).isNull();
  }

  @Test
  void process_ping_returnsEmptySuccessResult() {
    Map<String, Object> request = Map.of("jsonrpc", "2.0", "method", "ping", "id", 106);

    Map<String, Object> response = dispatcher.process(SESSION_ID, request);

    assertThat(response).containsKey("result");
    assertThat((Map<?, ?>) response.get("result")).isEmpty();
  }

  @Test
  void dispatch_delegatesToProcessAndSendsResultViaSessionManager_unchangedBehaviour()
      throws Exception {
    // Regression guard for the process()/dispatch() refactor: dispatch() must still push the exact
    // same payload process() computes through sessionManager.send(), preserving the legacy SSE
    // transport's externally-observable behaviour byte-for-byte.
    Map<String, Object> request = Map.of("jsonrpc", "2.0", "method", "ping", "id", 107);

    var captured = new HashMap<String, Object>();
    org.mockito.Mockito.doAnswer(inv -> {
      captured.putAll((Map<String, Object>) inv.getArgument(1));
      return null;
    }).when(sessionManager).send(org.mockito.ArgumentMatchers.eq(SESSION_ID),
        org.mockito.ArgumentMatchers.any());

    dispatcher.dispatch(SESSION_ID, request);

    assertThat(captured).isEqualTo(dispatcher.process(SESSION_ID, request));
  }
}
