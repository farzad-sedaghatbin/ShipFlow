package com.github.farzadsedaghatbin.shipflow.event;

/**
 * Published after a {@code DashboardNotification} row is committed, so side effects that need
 * the committed row (e.g. Web Push delivery) can run in the background instead of inline in
 * {@code DashboardNotificationService.createNotification}.
 *
 * @param notificationId the id of the freshly created notification
 */
public record NotificationCreatedEvent(Long notificationId) {}
