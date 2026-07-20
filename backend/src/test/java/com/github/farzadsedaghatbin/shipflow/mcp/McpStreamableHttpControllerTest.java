package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.controller.mcp.McpStreamableHttpController;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpServerSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link McpStreamableHttpController} — the MCP 2025-06-18 Streamable HTTP
 * transport. Uses a real {@link McpSessionManager} (cheap — just an in-memory map + a fresh
 * ObjectMapper) so session creation/lookup/removal is exercised for real, and mocks {@link
 * McpToolDispatcher} since JSON-RPC method routing is already covered by {@code
 * McpToolDispatcherTest}.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class McpStreamableHttpControllerTest {

  @Mock private McpToolDispatcher dispatcher;
  @Mock private McpServerSettingsService serverSettings;

  private McpSessionManager sessionManager;
  private McpStreamableHttpController controller;

  private static Authentication authFor(String username) {
    return new UsernamePasswordAuthenticationToken(username, null, List.of());
  }

  private static void setAuth(Authentication auth) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
  }

  @BeforeEach
  void setUp() {
    sessionManager = new McpSessionManager(new ObjectMapper());
    controller = new McpStreamableHttpController(dispatcher, sessionManager, serverSettings);
    when(serverSettings.isEnabled()).thenReturn(true);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ── full session lifecycle: init -> notify -> tool call -> terminate ──────────

  @Test
  void fullSessionLifecycle_initializeNotifyToolCallTerminate() {
    setAuth(authFor("alice"));

    // 1. initialize — no session header yet, server creates one
    Map<String, Object> initRequest = Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1);
    Map<String, Object> initResult =
        Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of("protocolVersion", "2024-11-05"));
    when(dispatcher.process(anyString(), eq(initRequest))).thenReturn(initResult);

    ResponseEntity<?> initResponse = controller.handleRequest(null, null, initRequest);

    assertThat(initResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    String sessionId = initResponse.getHeaders().getFirst("Mcp-Session-Id");
    assertThat(sessionId).isNotBlank();
    assertThat(initResponse.getBody()).isEqualTo(initResult);
    assertThat(sessionManager.get(sessionId)).isPresent();

    // 2. notifications/initialized — no id, dispatcher.process returns null (no reply expected)
    Map<String, Object> notifyRequest = Map.of("jsonrpc", "2.0", "method", "notifications/initialized");
    when(dispatcher.process(eq(sessionId), eq(notifyRequest))).thenReturn(null);

    ResponseEntity<?> notifyResponse = controller.handleRequest(null, sessionId, notifyRequest);

    assertThat(notifyResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(notifyResponse.getBody()).isNull();
    assertThat(notifyResponse.getHeaders().getFirst("Mcp-Session-Id")).isEqualTo(sessionId);

    // 3. tools/call — using the session established at init
    Map<String, Object> callRequest = Map.of(
        "jsonrpc", "2.0", "method", "tools/call",
        "params", Map.of("name", "list_projects", "arguments", Map.of()), "id", 2);
    Map<String, Object> callResult =
        Map.of("jsonrpc", "2.0", "id", 2, "result", Map.of("content", List.of(), "isError", false));
    when(dispatcher.process(eq(sessionId), eq(callRequest))).thenReturn(callResult);

    ResponseEntity<?> callResponse = controller.handleRequest(null, sessionId, callRequest);

    assertThat(callResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(callResponse.getBody()).isEqualTo(callResult);
    assertThat(callResponse.getHeaders().getFirst("Mcp-Session-Id")).isEqualTo(sessionId);

    // 4. DELETE — explicit session termination
    ResponseEntity<Void> deleteResponse = controller.terminateSession(null, sessionId);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(sessionManager.get(sessionId)).isEmpty();
  }

  // ── POST — session header handling ─────────────────────────────────────────

  @Test
  void post_nonInitializeWithoutSessionHeader_returns400() {
    setAuth(authFor("alice"));
    Map<String, Object> request = Map.of(
        "jsonrpc", "2.0", "method", "tools/list", "id", 5);

    ResponseEntity<?> response = controller.handleRequest(null, null, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void post_unknownSessionId_returns404() {
    setAuth(authFor("alice"));
    Map<String, Object> request = Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 6);

    ResponseEntity<?> response = controller.handleRequest(null, "no-such-session", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void post_differentPrincipalThanSessionOwner_returns403() {
    setAuth(authFor("alice"));
    Map<String, Object> initRequest = Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1);
    when(dispatcher.process(anyString(), eq(initRequest)))
        .thenReturn(Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of()));
    ResponseEntity<?> initResponse = controller.handleRequest(null, null, initRequest);
    String sessionId = initResponse.getHeaders().getFirst("Mcp-Session-Id");

    // A different authenticated principal tries to use alice's session.
    setAuth(authFor("mallory"));
    Map<String, Object> followUp = Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 2);

    ResponseEntity<?> response = controller.handleRequest(null, sessionId, followUp);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void post_sameSessionOwner_isAllowed() {
    setAuth(authFor("alice"));
    Map<String, Object> initRequest = Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1);
    when(dispatcher.process(anyString(), eq(initRequest)))
        .thenReturn(Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of()));
    ResponseEntity<?> initResponse = controller.handleRequest(null, null, initRequest);
    String sessionId = initResponse.getHeaders().getFirst("Mcp-Session-Id");

    Map<String, Object> followUp = Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 2);
    Map<String, Object> followUpResult = Map.of("jsonrpc", "2.0", "id", 2, "result", Map.of("tools", List.of()));
    when(dispatcher.process(eq(sessionId), eq(followUp))).thenReturn(followUpResult);

    ResponseEntity<?> response = controller.handleRequest(null, sessionId, followUp);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(followUpResult);
  }

  // ── GET — 405, optional server->client streaming not implemented ──────────────

  @Test
  void get_returns405() {
    setAuth(authFor("alice"));
    ResponseEntity<Void> response = controller.streamNotSupported(null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  // ── DELETE — missing / unknown session ─────────────────────────────────────

  @Test
  void delete_missingSessionHeader_returns400() {
    ResponseEntity<Void> response = controller.terminateSession(null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void delete_unknownSessionId_returns400() {
    ResponseEntity<Void> response = controller.terminateSession(null, "no-such-session");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  // ── MCP server disabled ─────────────────────────────────────────────────────

  @Test
  void post_serverDisabled_throws503() {
    when(serverSettings.isEnabled()).thenReturn(false);
    Map<String, Object> request = Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1);

    assertThatThrownBy(() -> controller.handleRequest(null, null, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void get_serverDisabled_throws503() {
    when(serverSettings.isEnabled()).thenReturn(false);

    assertThatThrownBy(() -> controller.streamNotSupported(null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void delete_serverDisabled_throws503() {
    when(serverSettings.isEnabled()).thenReturn(false);

    assertThatThrownBy(() -> controller.terminateSession(null, "some-session"))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("statusCode", HttpStatus.SERVICE_UNAVAILABLE);
  }

  // ── path-token variant — apiKeyPathToken is accepted but auth already happened upstream ───────

  @Test
  void post_withApiKeyPathToken_stillWorks() {
    setAuth(authFor("alice"));
    Map<String, Object> initRequest = Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1);
    Map<String, Object> initResult = Map.of("jsonrpc", "2.0", "id", 1, "result", Map.of());
    when(dispatcher.process(anyString(), eq(initRequest))).thenReturn(initResult);

    ResponseEntity<?> response = controller.handleRequest("sf_live_alice", null, initRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(initResult);
  }
}
