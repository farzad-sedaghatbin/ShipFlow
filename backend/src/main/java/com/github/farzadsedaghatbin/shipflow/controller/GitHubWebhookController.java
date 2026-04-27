package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.github.GitHubWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Webhook", description = "Endpoint for receiving GitHub webhook events")
public class GitHubWebhookController {

  private final GitHubWebhookService webhookService;

  @Value("${github.webhook.secret:}")
  private String webhookSecret;

  @PostMapping
  @Operation(summary = "Receive GitHub webhook events", description = "Processes GitHub webhook events for push, pull_request, and branch operations")
  public ResponseEntity<Map<String, String>> handleWebhook(@RequestHeader("X-GitHub-Event") String eventType,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
      @RequestBody String payload) {

    log.info("Received GitHub webhook event: {}", eventType);

    try {
      // Validate signature if webhook secret is configured
      if (webhookSecret != null && !webhookSecret.isEmpty()) {
        if (signature == null || signature.isEmpty()) {
          log.warn("Missing signature for webhook event");
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing signature"));
        }

        if (!webhookService.validateSignature(payload, signature, webhookSecret)) {
          log.warn("Invalid signature for webhook event");
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
        }
      }

      webhookService.processWebhook(eventType, payload);

      return ResponseEntity.ok(Map.of("status", "success"));
    } catch (Exception e) {
      log.error("Error processing webhook", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
