package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpServerSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * MCP Streamable HTTP transport endpoint (MCP spec 2025-06-18).
 *
 * <p>Unlike the legacy HTTP+SSE transport ({@code GET /mcp/sse} + {@code POST /mcp/messages}, see
 * {@link McpSseController} / {@link McpMessageController}), this transport exposes a single
 * endpoint supporting {@code POST}, {@code GET}, and {@code DELETE}. It exists because claude.ai's
 * hosted "custom connector" client is built primarily against Streamable HTTP: per the transport
 * spec's backwards-compatibility algorithm, a compliant client POSTs an {@code InitializeRequest}
 * directly to the configured URL, and a server that only supports {@code GET} (like {@code
 * /mcp/sse}) is not guaranteed to be retried on the legacy transport. Both transports remain fully
 * supported side-by-side — this is additive, not a replacement.
 *
 * <p>Only the synchronous {@code application/json} response path is implemented for {@code POST}
 * requests carrying a JSON-RPC request (spec: "The client MUST support both these cases", meaning
 * a server that only ever responds with a direct JSON body is fully compliant). Server-initiated
 * SSE streaming from {@code GET} is optional per spec and not implemented — {@code GET} responds
 * {@code 405 Method Not Allowed}.
 *
 * <p>Session management mirrors the legacy transport: {@link McpSessionManager#create(Authentication)}
 * is called on the first {@code initialize} request (rather than at connect time, since there is no
 * connect step here), and the resulting session ID is returned via the {@code Mcp-Session-Id}
 * response header. Every subsequent request must send that header back; the caller's authenticated
 * principal must match the session's owner (same session-hijack guard as {@link
 * McpMessageController}).
 *
 * <p>Two auth transports are supported (see {@code McpAuthFilter} for details): the primary {@code
 * Authorization: Bearer <api-key>} header at {@code /mcp}, and a secondary URL path-token transport
 * at {@code /mcp/<api-key>} for clients that cannot set custom headers — always capped to read-only
 * regardless of the key's real scopes.
 *
 * <p>Per spec this is lenient in two places, intentionally: the {@code MCP-Protocol-Version} header
 * is not read or validated (the spec's enforcement language is {@code SHOULD}, and a missing header
 * has a defined fallback), and {@code Origin} header / DNS-rebinding validation is skipped — that
 * guidance targets MCP servers bound to {@code localhost} that a malicious webpage's JS could probe
 * via the victim's browser, a threat model that doesn't apply to ShipFlow's hosted, authenticated,
 * multi-tenant API server (Bearer/path-token auth is the real access boundary here).
 *
 * <p>The MCP server is enabled/disabled at runtime via {@link McpServerSettingsService} (admin
 * toggle, falling back to the {@code mcp.server.enabled} environment default). When disabled, this
 * endpoint responds {@code 503 Service Unavailable}.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpStreamableHttpController {

  private static final String SESSION_HEADER = "Mcp-Session-Id";

  private final McpToolDispatcher dispatcher;
  private final McpSessionManager sessionManager;
  private final McpServerSettingsService serverSettings;

  @PostMapping({"", "/{apiKeyPathToken}"})
  public ResponseEntity<?> handleRequest(
      @PathVariable(required = false) String apiKeyPathToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String sessionIdHeader,
      @RequestBody Map<String, Object> request) {

    requireEnabled();

    String method = (String) request.get("method");
    boolean isInitialize = "initialize".equals(method);
    Authentication requestAuth = SecurityContextHolder.getContext().getAuthentication();

    McpSession session;
    if (isInitialize) {
      // No session yet — the server assigns one on initialize, same as the legacy transport's
      // GET /mcp/sse does at connect time.
      session = sessionManager.create(requestAuth);
      log.info("MCP Streamable HTTP session created: {}", session.getSessionId());
    } else {
      if (sessionIdHeader == null || sessionIdHeader.isBlank()) {
        return ResponseEntity.badRequest().build();
      }
      Optional<McpSession> found = sessionManager.get(sessionIdHeader);
      if (found.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      session = found.get();

      // Session-hijack guard — mirrors McpMessageController's existing check for the legacy
      // transport: the authenticated caller on this request must own the target session.
      Authentication sessionAuth = session.getAuth();
      if (requestAuth == null
          || sessionAuth == null
          || !requestAuth.getName().equals(sessionAuth.getName())) {
        log.warn(
            "MCP Streamable HTTP: session ownership mismatch — request={} session={}",
            requestAuth != null ? requestAuth.getName() : "null",
            sessionAuth != null ? sessionAuth.getName() : "null");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }
    }

    Map<String, Object> response = dispatcher.process(session.getSessionId(), request);

    if (response == null) {
      // Notification (e.g. notifications/initialized) or an id-less unknown method — no JSON-RPC
      // reply expected. Spec: respond 202 Accepted with no body.
      return ResponseEntity.accepted().header(SESSION_HEADER, session.getSessionId()).build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(SESSION_HEADER, session.getSessionId())
        .body(response);
  }

  /**
   * The optional server-initiated SSE stream is not implemented (spec: servers MAY offer it, and
   * a server that never does is compliant as long as it responds {@code 405}).
   */
  @GetMapping({"", "/{apiKeyPathToken}"})
  public ResponseEntity<Void> streamNotSupported(
      @PathVariable(required = false) String apiKeyPathToken) {
    requireEnabled();
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
  }

  @DeleteMapping({"", "/{apiKeyPathToken}"})
  public ResponseEntity<Void> terminateSession(
      @PathVariable(required = false) String apiKeyPathToken,
      @RequestHeader(value = SESSION_HEADER, required = false) String sessionIdHeader) {

    requireEnabled();

    if (sessionIdHeader == null || sessionIdHeader.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    if (sessionManager.get(sessionIdHeader).isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    sessionManager.remove(sessionIdHeader);
    log.info("MCP Streamable HTTP session terminated: {}", sessionIdHeader);
    return ResponseEntity.noContent().build();
  }

  private void requireEnabled() {
    if (!serverSettings.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "MCP server is disabled on this ShipFlow instance");
    }
  }
}
