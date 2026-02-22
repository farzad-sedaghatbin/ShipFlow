package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.WebhookPayload;
import com.github.farzadsedaghatbin.shipflow.entity.WebhookSubscription;
import com.github.farzadsedaghatbin.shipflow.repository.WebhookSubscriptionRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivers webhook payloads to subscriber endpoints.
 * Each delivery is signed with HMAC-SHA256 using the subscription's secret.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryService {

  private static final int MAX_FAILURES_BEFORE_DISABLE = 10;
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

  private final WebhookSubscriptionRepository subscriptionRepository;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(HTTP_TIMEOUT)
      .build();

  /**
   * Dispatches an event to all active subscriptions that match the event type.
   */
  @Async
  public void dispatch(String eventType, Map<String, Object> data) {
    List<WebhookSubscription> active = subscriptionRepository.findByIsActiveTrue();
    for (WebhookSubscription sub : active) {
      if (sub.matchesEvent(eventType)) {
        deliver(sub, eventType, data);
      }
    }
  }

  private void deliver(WebhookSubscription sub, String eventType, Map<String, Object> data) {
    try {
      WebhookPayload payload = WebhookPayload.builder()
          .id(UUID.randomUUID().toString())
          .eventType(eventType)
          .timestamp(LocalDateTime.now())
          .data(data)
          .build();

      String json = objectMapper.writeValueAsString(payload);
      String signature = hmacSha256(json, sub.getSecret());

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(sub.getTargetUrl()))
          .timeout(HTTP_TIMEOUT)
          .header("Content-Type", "application/json")
          .header("X-ShipFlow-Signature", "sha256=" + signature)
          .header("X-ShipFlow-Event", eventType)
          .header("X-ShipFlow-Delivery", payload.getId())
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        log.debug("Webhook delivered to {} (status={})", sub.getTargetUrl(), response.statusCode());
        resetFailureCount(sub);
      } else {
        log.warn("Webhook delivery to {} returned status {}", sub.getTargetUrl(), response.statusCode());
        incrementFailure(sub);
      }
    } catch (Exception e) {
      log.error("Webhook delivery failed for subscription {} ({}): {}",
          sub.getId(), sub.getTargetUrl(), e.getMessage());
      incrementFailure(sub);
    }
  }

  @Transactional
  void resetFailureCount(WebhookSubscription sub) {
    sub.setFailureCount(0);
    subscriptionRepository.save(sub);
  }

  @Transactional
  void incrementFailure(WebhookSubscription sub) {
    sub.setFailureCount(sub.getFailureCount() + 1);
    if (sub.getFailureCount() >= MAX_FAILURES_BEFORE_DISABLE) {
      sub.setIsActive(false);
      log.warn("Webhook subscription {} disabled after {} consecutive failures",
          sub.getId(), MAX_FAILURES_BEFORE_DISABLE);
    }
    subscriptionRepository.save(sub);
  }

  // ──────────────────────────── HMAC ────────────────────────────

  static String hmacSha256(String data, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 computation failed", e);
    }
  }
}
