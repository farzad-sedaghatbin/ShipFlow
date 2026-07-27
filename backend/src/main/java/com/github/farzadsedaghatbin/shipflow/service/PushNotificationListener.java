package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.DashboardNotification;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserPreference;
import com.github.farzadsedaghatbin.shipflow.event.NotificationCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardNotificationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers a Web Push notification whenever a {@code DashboardNotification} is created, as a
 * cross-cutting side effect wired through {@link NotificationCreatedEvent} rather than a direct
 * call from {@code DashboardNotificationService} (per the CLAUDE.md event-publisher rule for
 * side effects).
 *
 * <p>Runs <em>after</em> the publishing transaction commits ({@link TransactionPhase#AFTER_COMMIT})
 * so the reload of the notification sees the committed row — not a stale/empty read. A plain
 * {@code @Async @EventListener} would fire on a separate thread before
 * {@code DashboardNotificationService.createNotification}'s transaction commits, so
 * {@code findById} could return empty and the push would be silently skipped (same class of bug
 * documented on {@code WikiKnowledgeListener}/{@code ScopeProgressListener}). Because there is no
 * transaction to join after commit, the listener opens its own with
 * {@code Propagation.REQUIRES_NEW} — a plain {@code @Transactional} fails at startup for an
 * AFTER_COMMIT listener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationListener {

  private final DashboardNotificationRepository notificationRepository;
  private final UserPreferenceRepository userPreferenceRepository;
  private final IPushNotificationService pushNotificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onNotificationCreated(NotificationCreatedEvent event) {
    try {
      if (!pushNotificationService.isEnabled()) {
        return;
      }

      DashboardNotification notification =
          notificationRepository.findById(event.notificationId()).orElse(null);
      if (notification == null) {
        log.debug("Notification {} not found, skipping push delivery", event.notificationId());
        return;
      }

      User user = notification.getUser();
      if (user == null) {
        return;
      }

      if (!isPushEnabledForUser(user.getId())) {
        return;
      }

      pushNotificationService.sendNotification(
          user, notification.getTitle(), notification.getMessage(), notification.getActionUrl());
    } catch (Exception e) {
      log.error(
          "PushNotificationListener failed for notification {}: {}",
          event.notificationId(),
          e.getMessage(),
          e);
    }
  }

  /** Users without a preference row default to push-enabled (opt-out, not opt-in). */
  private boolean isPushEnabledForUser(Long userId) {
    return userPreferenceRepository
        .findByUserId(userId)
        .map(UserPreference::getPushEnabled)
        .map(enabled -> enabled == null || enabled)
        .orElse(true);
  }
}
