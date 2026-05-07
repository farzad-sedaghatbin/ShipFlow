package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateWebhookRequest;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.WebhookSubscriptionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WebhookSubscription;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WebhookSubscriptionRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WebhookSubscriptionService {

  private final WebhookSubscriptionRepository repository;
  private final UserRepository userRepository;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public WebhookSubscriptionDTO create(Long userId, CreateWebhookRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    String secret = generateSecret();

    WebhookSubscription sub = WebhookSubscription.builder()
        .name(request.getName())
        .targetUrl(request.getTargetUrl())
        .secret(secret)
        .eventTypes(request.getEventTypes())
        .user(user)
        .build();

    repository.save(sub);
    log.info("Created webhook subscription '{}' for user {}", request.getName(), userId);

    WebhookSubscriptionDTO dto = toDTO(sub);
    dto.setSecret(secret); // returned only on creation
    return dto;
  }

  @Transactional(readOnly = true)
  public List<WebhookSubscriptionDTO> listForUser(Long userId) {
    return repository.findByUserId(userId).stream()
        .map(this::toDTO)
        .toList();
  }

  public void delete(Long subscriptionId, Long userId) {
    WebhookSubscription sub = repository.findById(subscriptionId)
        .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + subscriptionId));
    if (!sub.getUser().getId().equals(userId)) {
      throw new SecurityException("Cannot delete another user's webhook");
    }
    repository.delete(sub);
    log.info("Deleted webhook subscription id={} for user {}", subscriptionId, userId);
  }

  public WebhookSubscriptionDTO toggleActive(Long subscriptionId, Long userId) {
    WebhookSubscription sub = repository.findById(subscriptionId)
        .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + subscriptionId));
    if (!sub.getUser().getId().equals(userId)) {
      throw new SecurityException("Cannot modify another user's webhook");
    }
    sub.setIsActive(!sub.getIsActive());
    sub.setFailureCount(0); // reset failures on re-enable
    repository.save(sub);
    return toDTO(sub);
  }

  // ──────────────────────────── helpers ────────────────────────────

  private String generateSecret() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private WebhookSubscriptionDTO toDTO(WebhookSubscription sub) {
    WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
    dto.setId(sub.getId());
    dto.setName(sub.getName());
    dto.setTargetUrl(sub.getTargetUrl());
    dto.setEventTypes(sub.getEventTypes());
    dto.setIsActive(sub.getIsActive());
    dto.setFailureCount(sub.getFailureCount());
    dto.setCreatedAt(sub.getCreatedAt());
    dto.setUpdatedAt(sub.getUpdatedAt());
    // secret intentionally omitted (only returned on create)
    return dto;
  }
}
