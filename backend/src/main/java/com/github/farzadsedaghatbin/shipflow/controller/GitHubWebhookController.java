package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.github.GitHubWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Webhook", description = "Endpoint for receiving GitHub webhook events")
public class GitHubWebhookController {

    private final GitHubWebhookService webhookService;

    @PostMapping
    @Operation(summary = "Receive GitHub webhook events",
               description = "Processes GitHub webhook events for push, pull_request, and branch operations")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {
        
        log.info("Received GitHub webhook event: {}", eventType);

        try {
            // Note: Signature validation can be added here if webhook secret is configured
            // For now, we'll process all events
            // TODO: Implement signature validation for production use
            
            webhookService.processWebhook(eventType, payload);
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.internalServerError()
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
}
