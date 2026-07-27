package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscribeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscriptionResponse;
import com.github.farzadsedaghatbin.shipflow.dto.push.VapidPublicKeyResponse;
import com.github.farzadsedaghatbin.shipflow.entity.PushSubscription;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.PushSubscriptionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages per-browser Web Push subscriptions for the current user. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PushSubscriptionService {

  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final UserRepository userRepository;
  private final IPushNotificationService pushNotificationService;

  /** Returns the server's VAPID public key so the frontend can create a subscription. */
  @Transactional(readOnly = true)
  public VapidPublicKeyResponse getVapidPublicKey() {
    return VapidPublicKeyResponse.builder()
        .publicKey(pushNotificationService.getVapidPublicKey())
        .enabled(pushNotificationService.isEnabled())
        .build();
  }

  /**
   * Register (or re-register) a browser subscription for the given user. A subscription
   * endpoint is globally unique per browser, so re-subscribing the same browser (e.g. after the
   * push service rotated its endpoint) upserts the existing row rather than creating a
   * duplicate.
   */
  public PushSubscriptionResponse subscribe(Long userId, PushSubscribeRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

    PushSubscription subscription =
        pushSubscriptionRepository
            .findByEndpoint(request.getEndpoint())
            .orElseGet(PushSubscription::new);

    subscription.setUser(user);
    subscription.setEndpoint(request.getEndpoint());
    subscription.setP256dhKey(request.getP256dhKey());
    subscription.setAuthKey(request.getAuthKey());
    subscription.setUserAgent(request.getUserAgent());
    subscription.setLastUsedAt(OffsetDateTime.now());

    PushSubscription saved = pushSubscriptionRepository.save(subscription);
    log.info("Push subscription registered for user {} (subscription {})", userId, saved.getId());

    return PushSubscriptionResponse.builder()
        .id(saved.getId())
        .endpoint(saved.getEndpoint())
        .createdAt(saved.getCreatedAt())
        .build();
  }

  /**
   * Remove a browser subscription. A push endpoint is not enumerable/guessable, so an endpoint
   * is effectively its own proof of ownership; we still scope the delete to the current user
   * when the row is found, as a cheap extra ownership check.
   */
  public void unsubscribe(Long userId, String endpoint) {
    pushSubscriptionRepository
        .findByEndpoint(endpoint)
        .ifPresent(
            subscription -> {
              if (subscription.getUser() != null
                  && !subscription.getUser().getId().equals(userId)) {
                log.warn(
                    "User {} attempted to unsubscribe endpoint owned by user {}",
                    userId,
                    subscription.getUser().getId());
                return;
              }
              pushSubscriptionRepository.deleteByEndpoint(endpoint);
              log.info("Push subscription removed for user {}", userId);
            });
  }
}
