package com.github.farzadsedaghatbin.shipflow.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.PushSubscription;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.PushSubscriptionRepository;
import com.github.farzadsedaghatbin.shipflow.service.IPushNotificationService;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Real Web Push implementation. Delegates RFC 8291 payload encryption and RFC 8292 VAPID
 * signing entirely to {@code nl.martijndwars:web-push} — no hand-rolled crypto here. Only
 * registered when {@code app.push.vapid.public-key} is configured; see
 * {@link NoOpPushNotificationService} for the fallback.
 */
@Service
@ConditionalOnProperty(name = "app.push.vapid.public-key")
@Slf4j
public class WebPushNotificationService implements IPushNotificationService {

  static {
    // Registered once per classloader at class-load time (rather than only inside init()) so
    // EC key parsing/decoding — used by Subscription/Notification construction whenever a push
    // is sent, not just during PushService setup — always has the "BC" provider available.
    Security.addProvider(new BouncyCastleProvider());
  }

  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${app.push.vapid.public-key}")
  private String vapidPublicKey;

  @Value("${app.push.vapid.private-key:}")
  private String vapidPrivateKey;

  @Value("${app.push.vapid.subject:mailto:noreply@shipflow.dev}")
  private String vapidSubject;

  /** Null until {@link #init()} succeeds; {@link #isEnabled()} reflects that. */
  private PushService pushService;

  public WebPushNotificationService(PushSubscriptionRepository pushSubscriptionRepository) {
    this.pushSubscriptionRepository = pushSubscriptionRepository;
  }

  @PostConstruct
  void init() {
    // app.push.vapid.public-key is defined with a blank default (${VAPID_PUBLIC_KEY:}, same
    // convention as spring.mail.host), so this bean is always the one registered for
    // IPushNotificationService — NoOpPushNotificationService's @ConditionalOnMissingBean is not
    // reliably ordered against a plain @Service (unlike auto-configuration classes), so we must
    // never let a blank/malformed key crash @PostConstruct; isEnabled() is the real on/off switch.
    if (vapidPublicKey == null || vapidPublicKey.isBlank()) {
      log.info("Web Push notifications disabled — app.push.vapid.public-key not configured");
      this.pushService = null;
      return;
    }

    try {
      this.pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
      log.info("Web Push notifications enabled (VAPID subject: {})", vapidSubject);
    } catch (GeneralSecurityException | RuntimeException e) {
      // BouncyCastle's EC point decoding throws unchecked exceptions for a malformed key —
      // never let a bad key crash application startup.
      log.error(
          "Invalid VAPID keypair configured under app.push.vapid.* — push notifications disabled",
          e);
      this.pushService = null;
    }
  }

  @Override
  public boolean isEnabled() {
    return pushService != null;
  }

  @Override
  public String getVapidPublicKey() {
    return vapidPublicKey;
  }

  @Override
  public void sendNotification(User user, String title, String body, String actionUrl) {
    if (!isEnabled() || user == null || user.getId() == null) {
      return;
    }

    List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(user.getId());
    if (subscriptions.isEmpty()) {
      return;
    }

    String payload = buildPayload(title, body, actionUrl);
    if (payload == null) {
      return;
    }

    for (PushSubscription subscription : subscriptions) {
      sendToSubscription(subscription, payload);
    }
  }

  private String buildPayload(String title, String body, String actionUrl) {
    try {
      Map<String, String> data = new LinkedHashMap<>();
      data.put("title", title != null ? title : "ShipFlow");
      data.put("body", body != null ? body : "");
      data.put("url", actionUrl != null ? actionUrl : "/");
      return objectMapper.writeValueAsString(data);
    } catch (Exception e) {
      log.error("Failed to serialize Web Push payload", e);
      return null;
    }
  }

  /**
   * Send to a single subscription. Deliberately never throws — a push delivery failure must
   * never break the notification-creation flow this is attached to (it runs from an
   * {@code AFTER_COMMIT} listener, well past the point where the caller could still roll back).
   */
  private void sendToSubscription(PushSubscription subscription, String payload) {
    try {
      Subscription webPushSubscription =
          new Subscription(
              subscription.getEndpoint(),
              new Subscription.Keys(subscription.getP256dhKey(), subscription.getAuthKey()));
      Notification notification = new Notification(webPushSubscription, payload);

      HttpResponse response = pushService.send(notification);
      int status = response.getStatusLine().getStatusCode();

      if (status == 404 || status == 410) {
        // The browser unsubscribed or the endpoint died — keep it around and every future
        // notification wastes a network round-trip for nothing.
        log.info(
            "Push subscription {} is gone (HTTP {}); removing", subscription.getId(), status);
        pushSubscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
      } else if (status >= 300) {
        log.warn(
            "Push delivery to subscription {} returned HTTP {}", subscription.getId(), status);
      } else {
        log.debug("Push notification delivered to subscription {}", subscription.getId());
      }
    } catch (Exception e) {
      log.warn(
          "Failed to deliver push notification to subscription {}: {}",
          subscription.getId(),
          e.getMessage());
    }
  }
}
