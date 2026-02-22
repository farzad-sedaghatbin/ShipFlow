package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.inbound.InboundWebhookRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Vendor-agnostic inbound webhook endpoint.
 *
 * <p>External services (Intercom, Zendesk, PagerDuty, Jira, linear, etc.)
 * POST events to {@code /api/inbound/{provider}} and the router dispatches
 * to the matching {@link com.github.farzadsedaghatbin.shipflow.service.inbound.InboundWebhookHandler}.</p>
 *
 * <p>No authentication filter is applied – each handler validates its own
 * provider-specific signature.</p>
 *
 * @since 0.6.0
 */
@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inbound Webhooks", description = "Receive events from external services")
public class InboundWebhookController {

  private final InboundWebhookRouter router;

  /**
   * Receive an inbound event from any registered provider.
   *
   * <p>URL pattern: {@code POST /api/inbound/{provider}}</p>
   * <p>Example: {@code POST /api/inbound/intercom}</p>
   */
  @PostMapping("/{provider}")
  @Operation(summary = "Receive an inbound webhook event",
      description = "Dispatches to the registered handler for the given provider. "
          + "Signature validation is performed by the handler, not the controller.")
  public ResponseEntity<Map<String, Object>> receive(
      @PathVariable String provider,
      @RequestHeader(value = "X-Event-Type", required = false) String eventTypeHeader,
      @RequestBody String payload,
      HttpServletRequest request) {

    log.info("Inbound webhook received: provider={}, contentLength={}",
        provider, payload.length());

    // Collect headers into a simple map for handlers
    Map<String, String> headers = extractHeaders(request);

    // Derive event type: explicit header → provider-specific header → "unknown"
    String eventType = resolveEventType(eventTypeHeader, headers, provider);

    Map<String, Object> result = router.route(provider, eventType, payload, headers);

    String status = String.valueOf(result.getOrDefault("status", "error"));
    return switch (status) {
      case "success", "accepted", "created" -> ResponseEntity.ok(result);
      case "error" -> {
        String msg = String.valueOf(result.getOrDefault("message", ""));
        if (msg.contains("Unknown provider")) {
          yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } else if (msg.contains("Invalid signature")) {
          yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        } else if (msg.contains("not active")) {
          yield ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
        yield ResponseEntity.badRequest().body(result);
      }
      default -> ResponseEntity.ok(result);
    };
  }

  /**
   * List active inbound providers.
   */
  @GetMapping
  @Operation(summary = "List active inbound webhook providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    List<String> providers = router.listProviders();
    return ResponseEntity.ok(Map.of("providers", providers));
  }

  // ── helpers ──────────────────────────────────────────────────────────

  private Map<String, String> extractHeaders(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      headers.put(name.toLowerCase(), request.getHeader(name));
    }
    return headers;
  }

  /**
   * Resolve event type from common header conventions.
   */
  private String resolveEventType(String explicit, Map<String, String> headers, String provider) {
    if (explicit != null && !explicit.isBlank()) {
      return explicit;
    }
    // Provider-specific header conventions
    String[] knownEventHeaders = {
        "x-event-type",         // Generic
        "x-github-event",      // GitHub
        "x-intercom-event",    // Intercom (hypothetical)
        "x-hook-event",        // Zendesk
        "x-pagerduty-event",   // PagerDuty
        "x-gitlab-event",      // GitLab
        "x-linear-event",      // Linear
    };
    for (String header : knownEventHeaders) {
      String value = headers.get(header);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "unknown";
  }
}
