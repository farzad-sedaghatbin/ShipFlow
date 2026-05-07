package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateWebhookRequest;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.WebhookSubscriptionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.WebhookSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Manages webhook subscriptions. Requires JWT authentication (not API key).
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook Management", description = "Create, list, toggle, and delete webhook subscriptions")
public class WebhookController {

  private final WebhookSubscriptionService webhookService;
  private final UserRepository userRepository;

  @PostMapping
  @Operation(summary = "Create a new webhook subscription – secret is returned only once")
  public ResponseEntity<WebhookSubscriptionDTO> create(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody CreateWebhookRequest request) {
    User user = resolveUser(principal);
    WebhookSubscriptionDTO dto = webhookService.create(user.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @GetMapping
  @Operation(summary = "List webhook subscriptions for the current user")
  public ResponseEntity<List<WebhookSubscriptionDTO>> list(
      @AuthenticationPrincipal UserDetails principal) {
    User user = resolveUser(principal);
    return ResponseEntity.ok(webhookService.listForUser(user.getId()));
  }

  @PatchMapping("/{id}/toggle")
  @Operation(summary = "Toggle a webhook subscription active/inactive")
  public ResponseEntity<WebhookSubscriptionDTO> toggle(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable Long id) {
    User user = resolveUser(principal);
    return ResponseEntity.ok(webhookService.toggleActive(id, user.getId()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a webhook subscription")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable Long id) {
    User user = resolveUser(principal);
    webhookService.delete(id, user.getId());
    return ResponseEntity.noContent().build();
  }

  private User resolveUser(UserDetails principal) {
    return userRepository.findByUsername(principal.getUsername())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<Void> handleSecurityException(SecurityException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Void> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
