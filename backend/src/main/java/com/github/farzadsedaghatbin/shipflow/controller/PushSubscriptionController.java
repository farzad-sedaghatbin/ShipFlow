package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscribeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscriptionResponse;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushUnsubscribeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.push.VapidPublicKeyResponse;
import com.github.farzadsedaghatbin.shipflow.service.PushSubscriptionService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the current user's Web Push subscriptions. Self-service only —
 * a subscription is scoped to whichever user is authenticated, so no {@code @RequirePermission}/
 * RBAC gating is needed (mirrors {@code UserPreferenceController}).
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Web Push", description = "Manage the current user's Web Push notification subscriptions")
public class PushSubscriptionController {

  private final PushSubscriptionService pushSubscriptionService;
  private final UserService userService;

  @GetMapping("/vapid-public-key")
  @Operation(
      summary = "Get the server's VAPID public key",
      description = "Needed by the frontend to create a browser PushSubscription")
  public ResponseEntity<VapidPublicKeyResponse> getVapidPublicKey() {
    return ResponseEntity.ok(pushSubscriptionService.getVapidPublicKey());
  }

  @PostMapping("/subscribe")
  @Operation(
      summary = "Register a browser Web Push subscription",
      description = "Upserts by endpoint — re-subscribing the same browser updates the existing row")
  public ResponseEntity<PushSubscriptionResponse> subscribe(
      @Valid @RequestBody PushSubscribeRequest request) {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(pushSubscriptionService.subscribe(userId, request));
  }

  @DeleteMapping("/unsubscribe")
  @Operation(summary = "Remove a browser Web Push subscription")
  public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest request) {
    Long userId = getCurrentUserId();
    pushSubscriptionService.unsubscribe(userId, request.getEndpoint());
    return ResponseEntity.noContent().build();
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
