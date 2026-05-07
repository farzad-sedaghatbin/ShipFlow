package com.github.farzadsedaghatbin.shipflow.service.notification;

import com.github.farzadsedaghatbin.shipflow.entity.Person;

/**
 * Generic interface for notification providers (Slack, MS Teams, Discord, etc.)
 *
 * <p>Implementations encapsulate provider-specific logic for sending messages
 * and managing channel configurations. The application dispatches notifications
 * through this interface so that new providers can be plugged in without
 * modifying core business logic.</p>
 *
 * @since 0.6.0
 */
public interface NotificationProvider {

  /**
   * Returns the unique name of this notification provider (e.g. "slack",
   * "teams", "discord").
   */
  String getProviderName();

  /**
   * Send a notification message.
   *
   * @param notificationType type of event (e.g. "TASK_ASSIGNED",
   *                         "CYCLE_STARTED")
   * @param message          formatted message body
   * @param channel          target channel/conversation (nullable – uses
   *                         provider default)
   * @param entityType       type of the related entity (e.g. "TASK", "PITCH")
   * @param entityId         primary key of the related entity (nullable)
   */
  void sendNotification(String notificationType, String message,
      String channel, String entityType, Long entityId);

  /**
   * Returns {@code true} when this provider is configured and active.
   */
  boolean isActive();

  /**
   * Resolve a @mention string for the given person on this provider.
   * Returns {@code null} if no mapping exists or the provider does not
   * support mentions.
   *
   * @param person the person to mention (nullable)
   * @return provider-specific mention string (e.g. "&lt;@U12345&gt;" for
   *         Slack) or {@code null}
   */
  default String resolveUserMention(Person person) {
    return null;
  }
}
