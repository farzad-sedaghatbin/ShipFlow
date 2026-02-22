package com.github.farzadsedaghatbin.shipflow.service.inbound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Routes inbound webhook requests to the appropriate {@link InboundWebhookHandler}.
 *
 * <p>All handlers are auto-discovered via constructor injection (Spring collects
 * every bean that implements {@link InboundWebhookHandler}). Lookup is O(1) by
 * provider name.</p>
 *
 * @since 0.7.0
 */
@Service
@Slf4j
public class InboundWebhookRouter {

  private final Map<String, InboundWebhookHandler> handlers;

  public InboundWebhookRouter(List<InboundWebhookHandler> handlerList) {
    this.handlers = handlerList.stream()
        .collect(Collectors.toMap(
            h -> h.getProviderName().toLowerCase(),
            h -> h,
            (a, b) -> {
              log.warn("Duplicate inbound handler for provider '{}' – keeping first", a.getProviderName());
              return a;
            }));
    log.info("Registered {} inbound webhook handler(s): {}", handlers.size(), handlers.keySet());
  }

  /**
   * Find handler by provider name (case-insensitive).
   */
  public Optional<InboundWebhookHandler> resolve(String provider) {
    return Optional.ofNullable(handlers.get(provider.toLowerCase()));
  }

  /**
   * Returns all registered provider names.
   */
  public List<String> listProviders() {
    return handlers.entrySet().stream()
        .filter(e -> e.getValue().isActive())
        .map(Map.Entry::getKey)
        .sorted()
        .toList();
  }

  /**
   * Process an inbound event end-to-end: resolve → validate → handle.
   *
   * @return result map or an error map if provider unknown / signature invalid
   */
  public Map<String, Object> route(String provider, String eventType,
      String payload, Map<String, String> headers) {

    InboundWebhookHandler handler = resolve(provider)
        .orElse(null);

    if (handler == null) {
      log.warn("No inbound handler registered for provider '{}'", provider);
      return Map.of("status", "error",
          "message", "Unknown provider: " + provider);
    }

    if (!handler.isActive()) {
      log.warn("Inbound handler '{}' is not active", provider);
      return Map.of("status", "error",
          "message", "Provider '" + provider + "' is not active");
    }

    if (!handler.validateSignature(payload, headers)) {
      log.warn("Signature validation failed for provider '{}'", provider);
      return Map.of("status", "error",
          "message", "Invalid signature");
    }

    try {
      Map<String, Object> result = handler.handle(eventType, payload, headers);
      log.info("Inbound event processed: provider={}, eventType={}, status={}",
          provider, eventType, result.getOrDefault("status", "unknown"));
      return result;
    } catch (Exception e) {
      log.error("Error processing inbound event from '{}': {}", provider, e.getMessage(), e);
      return Map.of("status", "error",
          "message", "Processing failed: " + e.getMessage());
    }
  }
}
