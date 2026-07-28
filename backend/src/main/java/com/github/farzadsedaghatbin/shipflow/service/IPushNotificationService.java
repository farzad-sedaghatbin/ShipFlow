package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.User;

/**
 * Contract for Web Push notification delivery. A no-op stub is registered when VAPID keys are
 * not configured.
 */
public interface IPushNotificationService {

  /** Returns true when VAPID keys are configured and push sending is active. */
  boolean isEnabled();

  /**
   * The server's VAPID public key (base64url), which the frontend needs to create a
   * {@code PushSubscription} via the browser's Push API. Returns {@code null} when Web Push is
   * not configured.
   */
  String getVapidPublicKey();

  /**
   * Send a push notification to every browser subscription registered for the given user.
   * Delivery failures never throw — a dead/expired subscription is pruned automatically, and any
   * other failure is logged and swallowed so it can never break the caller's flow.
   *
   * @param user the recipient
   * @param title short notification title
   * @param body notification body text (may be blank)
   * @param actionUrl relative URL to open when the notification is clicked
   */
  void sendNotification(User user, String title, String body, String actionUrl);
}
