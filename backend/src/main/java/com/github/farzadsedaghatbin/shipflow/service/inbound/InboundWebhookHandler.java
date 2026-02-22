package com.github.farzadsedaghatbin.shipflow.service.inbound;

import java.util.Map;

/**
 * Generic contract for handling inbound webhook events from external services.
 *
 * <p>Implementations are provider-specific (Intercom, Zendesk, PagerDuty, etc.)
 * but the controller and router are completely vendor-agnostic. Each handler
 * self-registers by returning a unique {@link #getProviderName()} and the
 * router dispatches based on the URL path segment.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>External service POSTs to {@code /api/inbound/{provider}}</li>
 *   <li>{@link InboundWebhookRouter} finds the handler for {@code provider}</li>
 *   <li>Router calls {@link #validateSignature} – reject if invalid</li>
 *   <li>Router calls {@link #handle} – handler maps payload → ShipFlow action</li>
 * </ol>
 *
 * @since 0.7.0
 */
public interface InboundWebhookHandler {

  /**
   * Unique provider key used in the URL path (e.g. "intercom", "zendesk",
   * "pagerduty"). Must be lowercase, alphanumeric + hyphens.
   */
  String getProviderName();

  /**
   * Validate the request signature / shared secret.
   *
   * @param payload   raw request body
   * @param headers   HTTP headers (provider-specific signature header)
   * @return {@code true} if the request is authentic
   */
  boolean validateSignature(String payload, Map<String, String> headers);

  /**
   * Process the inbound event.
   *
   * @param eventType provider-specific event type (from header or payload)
   * @param payload   raw JSON body
   * @param headers   HTTP headers
   * @return result map with at least {@code "status"} key
   */
  Map<String, Object> handle(String eventType, String payload, Map<String, String> headers);

  /**
   * Returns {@code true} when this handler is configured and ready to
   * accept events.
   */
  boolean isActive();
}
