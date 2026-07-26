package com.github.farzadsedaghatbin.shipflow.service.push;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.service.IPushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op stub that is registered when VAPID keys are not configured. All methods are no-ops so
 * the application can start without Web Push settings.
 */
@Service
@ConditionalOnMissingBean(IPushNotificationService.class)
@Slf4j
public class NoOpPushNotificationService implements IPushNotificationService {

  public NoOpPushNotificationService() {
    log.info("Web Push notifications disabled — app.push.vapid.public-key not configured");
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public String getVapidPublicKey() {
    return null;
  }

  @Override
  public void sendNotification(User user, String title, String body, String actionUrl) {
    log.debug("Push notification suppressed (Web Push not configured): {}", title);
  }
}
