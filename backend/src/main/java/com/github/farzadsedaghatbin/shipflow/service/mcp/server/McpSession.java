package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Represents one active MCP client connection via SSE. */
@Getter
@RequiredArgsConstructor
public class McpSession {

  private final String sessionId;
  private final SseEmitter emitter;
  private final Authentication auth;
  private final Instant createdAt;
}
