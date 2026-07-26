package com.github.farzadsedaghatbin.shipflow.service.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.PushSubscription;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.PushSubscriptionRepository;
import java.io.IOException;
import java.util.List;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link WebPushNotificationService}. The underlying {@code PushService} is
 * mocked (never a real HTTP call, never real VAPID crypto) — every test constructs the service
 * directly and swaps its private {@code pushService} field via reflection, mirroring the
 * approach {@code EmailNotificationServiceTest} uses for {@code @Value}-bound fields.
 */
@ExtendWith(MockitoExtension.class)
class WebPushNotificationServiceTest {

  // Valid P-256 uncompressed public key point (base64url) — arbitrary throwaway test key,
  // required so nl.martijndwars.webpush.Notification's constructor can actually parse it into
  // an EC point (an arbitrary garbage string would fail before ever reaching PushService.send).
  private static final String P256DH =
      "BHJM7MPfh8QfBTttBPJRtODiIIycaIhfAJrj5DsxFOeaPZY3CRnJy9Yg16y34XQ0UAH0_WGmepAEr7qQccn3xOw";
  private static final String AUTH = "OlCx4OP97j6KT5yYYlmu1A";

  @Mock private PushSubscriptionRepository pushSubscriptionRepository;
  @Mock private PushService mockPushService;
  @Mock private HttpResponse httpResponse;
  @Mock private StatusLine statusLine;

  private WebPushNotificationService service;
  private User testUser;

  @BeforeEach
  void setUp() {
    service = new WebPushNotificationService(pushSubscriptionRepository);
    ReflectionTestUtils.setField(service, "pushService", mockPushService);
    testUser = User.builder().id(1L).username("alice").build();
  }

  // -----------------------------------------------------------------
  // isEnabled
  // -----------------------------------------------------------------

  @Test
  void isEnabled_true_whenPushServiceInitialized() {
    assertThat(service.isEnabled()).isTrue();
  }

  @Test
  void isEnabled_false_whenPushServiceNull() {
    ReflectionTestUtils.setField(service, "pushService", null);
    assertThat(service.isEnabled()).isFalse();
  }

  @Test
  void init_leavesPushServiceNull_whenPublicKeyBlank() {
    WebPushNotificationService fresh = new WebPushNotificationService(pushSubscriptionRepository);
    ReflectionTestUtils.setField(fresh, "vapidPublicKey", "");

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(fresh, "init")).doesNotThrowAnyException();

    assertThat(fresh.isEnabled()).isFalse();
  }

  @Test
  void init_leavesPushServiceNull_whenVapidKeysAreInvalid() {
    WebPushNotificationService fresh = new WebPushNotificationService(pushSubscriptionRepository);
    ReflectionTestUtils.setField(fresh, "vapidPublicKey", "not-a-valid-key");
    ReflectionTestUtils.setField(fresh, "vapidPrivateKey", "not-a-valid-key-either");
    ReflectionTestUtils.setField(fresh, "vapidSubject", "mailto:test@shipflow.dev");

    assertThatCode(() -> ReflectionTestUtils.invokeMethod(fresh, "init")).doesNotThrowAnyException();

    assertThat(fresh.isEnabled()).isFalse();
  }

  // -----------------------------------------------------------------
  // sendNotification — no subscriptions
  // -----------------------------------------------------------------

  @Test
  void sendNotification_noop_whenUserHasNoSubscriptions() {
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of());

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verifyNoInteractions(mockPushService);
  }

  @Test
  void sendNotification_noop_whenNotEnabled() {
    ReflectionTestUtils.setField(service, "pushService", null);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verifyNoInteractions(pushSubscriptionRepository);
  }

  // -----------------------------------------------------------------
  // sendNotification — expired subscription cleanup
  // -----------------------------------------------------------------

  @Test
  void sendNotification_removesSubscription_on410Gone() throws Exception {
    PushSubscription subscription = subscription(10L, "https://push.example.com/a");
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
    when(mockPushService.send(any(Notification.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(410);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verify(pushSubscriptionRepository).deleteByEndpoint("https://push.example.com/a");
  }

  @Test
  void sendNotification_removesSubscription_on404NotFound() throws Exception {
    PushSubscription subscription = subscription(11L, "https://push.example.com/b");
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
    when(mockPushService.send(any(Notification.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(404);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verify(pushSubscriptionRepository).deleteByEndpoint("https://push.example.com/b");
  }

  @Test
  void sendNotification_keepsSubscription_onSuccessfulDelivery() throws Exception {
    PushSubscription subscription = subscription(12L, "https://push.example.com/c");
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
    when(mockPushService.send(any(Notification.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(201);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verify(pushSubscriptionRepository, never()).deleteByEndpoint(anyString());
  }

  @Test
  void sendNotification_keepsSubscription_onServerError() throws Exception {
    PushSubscription subscription = subscription(13L, "https://push.example.com/d");
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
    when(mockPushService.send(any(Notification.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(500);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    verify(pushSubscriptionRepository, never()).deleteByEndpoint(anyString());
  }

  // -----------------------------------------------------------------
  // sendNotification — failures never propagate
  // -----------------------------------------------------------------

  @Test
  void sendNotification_doesNotThrow_whenPushServiceThrows() throws Exception {
    PushSubscription subscription = subscription(14L, "https://push.example.com/e");
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
    when(mockPushService.send(any(Notification.class))).thenThrow(new IOException("network down"));

    assertThatCode(() -> service.sendNotification(testUser, "Title", "Body", "/tasks/1"))
        .doesNotThrowAnyException();

    verify(pushSubscriptionRepository, never()).deleteByEndpoint(anyString());
  }

  @Test
  void sendNotification_doesNotThrow_whenSubscriptionKeysAreMalformed() {
    PushSubscription malformed =
        PushSubscription.builder()
            .id(15L)
            .endpoint("https://push.example.com/f")
            .p256dhKey("not-a-valid-ec-point")
            .authKey(AUTH)
            .build();
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(malformed));

    assertThatCode(() -> service.sendNotification(testUser, "Title", "Body", "/tasks/1"))
        .doesNotThrowAnyException();
  }

  @Test
  void sendNotification_continuesToNextSubscription_whenOneFails() throws Exception {
    PushSubscription good = subscription(20L, "https://push.example.com/good");
    PushSubscription bad =
        PushSubscription.builder()
            .id(21L)
            .endpoint("https://push.example.com/bad")
            .p256dhKey("garbage")
            .authKey(AUTH)
            .build();
    when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(bad, good));
    when(mockPushService.send(any(Notification.class))).thenReturn(httpResponse);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(201);

    service.sendNotification(testUser, "Title", "Body", "/tasks/1");

    // Only the well-formed subscription reaches PushService.send
    verify(mockPushService, times(1)).send(any(Notification.class));
  }

  @Test
  void getVapidPublicKey_returnsConfiguredValue() {
    ReflectionTestUtils.setField(service, "vapidPublicKey", "some-public-key");
    assertThat(service.getVapidPublicKey()).isEqualTo("some-public-key");
  }

  private PushSubscription subscription(Long id, String endpoint) {
    return PushSubscription.builder().id(id).endpoint(endpoint).p256dhKey(P256DH).authKey(AUTH).build();
  }
}
