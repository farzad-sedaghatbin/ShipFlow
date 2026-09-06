package com.github.farzadsedaghatbin.shipflow.service;

/**
 * Redis pub/sub payload used by {@link NotificationSseManager} to fan an SSE event out to every
 * backend pod. Package-private — an implementation detail of {@link NotificationSseManager}, never
 * referenced outside the {@code service} package.
 *
 * @param originInstanceId the publishing pod's {@link NotificationSseManager#instanceId}, used to
 *                          detect and ignore a pod's own message echoing back through its own
 *                          Redis subscription
 * @param userId            the target user's ID, or {@code null} to mean "broadcast to everyone"
 * @param eventName         the SSE event name
 * @param payload           the event payload
 */
record SseFanOutMessage(String originInstanceId, Long userId, String eventName, Object payload) {}
