package com.github.farzadsedaghatbin.shipflow.service.inbound;

import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Routes inbound webhook requests to the appropriate {@link InboundWebhookHandler}.
 *
 * <p>Handlers come from two sources:
 * <ol>
 *   <li><strong>Code-level</strong> — Spring beans implementing {@link InboundWebhookHandler}</li>
 *   <li><strong>DB-configured</strong> — Entries in the {@code inbound_webhook_config} table,
 *       handled by the {@link GenericInboundWebhookHandler} fallback</li>
 * </ol>
 * Code-level handlers always take precedence over DB configs for the same provider name.</p>
 *
 * @since 0.6.0
 */
@Service
@Slf4j
public class InboundWebhookRouter {

  private final Map<String, InboundWebhookHandler> handlers;
  private final GenericInboundWebhookHandler genericHandler;
  private final InboundWebhookConfigRepository configRepository;

  public InboundWebhookRouter(List<InboundWebhookHandler> handlerList,
      GenericInboundWebhookHandler genericHandler,
      InboundWebhookConfigRepository configRepository) {
    this.genericHandler = genericHandler;
    this.configRepository = configRepository;
    this.handlers = handlerList.stream()
        .collect(Collectors.toMap(
            h -> h.getProviderName().toLowerCase(),
            h -> h,
            (a, b) -> {
              log.warn("Duplicate inbound handler for provider '{}' – keeping first", a.getProviderName());
              return a;
            }));
    log.info("Registered {} code-level inbound webhook handler(s): {}", handlers.size(), handlers.keySet());
  }

  /**
   * Find code-level handler by provider name (case-insensitive).
   */
  public Optional<InboundWebhookHandler> resolve(String provider) {
    return Optional.ofNullable(handlers.get(provider.toLowerCase()));
  }

  /**
   * Returns all active provider names (code-level + DB-configured).
   */
  public List<String> listProviders() {
    TreeSet<String> providers = new TreeSet<>();

    // Code-level handlers
    handlers.entrySet().stream()
        .filter(e -> e.getValue().isActive())
        .map(Map.Entry::getKey)
        .forEach(providers::add);

    // DB-configured providers
    configRepository.findByIsEnabledTrue()
        .forEach(cfg -> providers.add(cfg.getProviderName().toLowerCase()));

    return new ArrayList<>(providers);
  }

  /**
   * Process an inbound event end-to-end: resolve → validate → handle.
   *
   * <p>First checks code-level handlers; if none found, falls back to
   * DB-configured providers via {@link GenericInboundWebhookHandler}.</p>
   *
   * @return result map or an error map if provider unknown / signature invalid
   */
  public Map<String, Object> route(String provider, String eventType,
      String payload, Map<String, String> headers) {

    // 1. Try code-level handler first
    InboundWebhookHandler handler = resolve(provider).orElse(null);
    if (handler != null) {
      return routeWithHandler(handler, provider, eventType, payload, headers);
    }

    // 2. Fall back to DB-configured generic handler
    if (genericHandler.supports(provider)) {
      if (!genericHandler.validateSignature(provider, payload, headers)) {
        log.warn("Signature validation failed for DB-configured provider '{}'", provider);
        return Map.of("status", "error", "message", "Invalid signature");
      }
      try {
        Map<String, Object> result = genericHandler.handle(provider, eventType, payload, headers);
        log.info("Inbound event processed (generic): provider={}, eventType={}, status={}",
            provider, eventType, result.getOrDefault("status", "unknown"));
        return result;
      } catch (Exception e) {
        log.error("Error processing inbound event from '{}' (generic): {}", provider, e.getMessage(), e);
        return Map.of("status", "error", "message", "Processing failed: " + e.getMessage());
      }
    }

    // 3. Unknown provider
    log.warn("No inbound handler registered for provider '{}'", provider);
    return Map.of("status", "error", "message", "Unknown provider: " + provider);
  }

  private Map<String, Object> routeWithHandler(InboundWebhookHandler handler,
      String provider, String eventType, String payload, Map<String, String> headers) {

    if (!handler.isActive()) {
      log.warn("Inbound handler '{}' is not active", provider);
      return Map.of("status", "error",
          "message", "Provider '" + provider + "' is not active");
    }

    if (!handler.validateSignature(payload, headers)) {
      log.warn("Signature validation failed for provider '{}'", provider);
      return Map.of("status", "error", "message", "Invalid signature");
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
