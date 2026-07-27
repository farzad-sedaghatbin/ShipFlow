package com.github.farzadsedaghatbin.shipflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.DashboardNotification;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserPreference;
import com.github.farzadsedaghatbin.shipflow.event.NotificationCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardNotificationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserPreferenceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationListenerTest {

  @Mock private DashboardNotificationRepository notificationRepository;
  @Mock private UserPreferenceRepository userPreferenceRepository;
  @Mock private IPushNotificationService pushNotificationService;

  private PushNotificationListener listener;

  private User testUser;
  private DashboardNotification testNotification;

  @BeforeEach
  void setUp() {
    listener =
        new PushNotificationListener(
            notificationRepository, userPreferenceRepository, pushNotificationService);

    testUser = User.builder().id(1L).username("alice").build();
    testNotification =
        DashboardNotification.builder()
            .id(100L)
            .user(testUser)
            .title("Task overdue")
            .message("Fix the bug is overdue")
            .actionUrl("/backlog/5")
            .build();
  }

  @Test
  void sendsPush_whenEnabledAndUserOptedIn() {
    when(pushNotificationService.isEnabled()).thenReturn(true);
    when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
    UserPreference preference = UserPreference.builder().user(testUser).pushEnabled(true).build();
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(preference));

    listener.onNotificationCreated(new NotificationCreatedEvent(100L));

    verify(pushNotificationService)
        .sendNotification(testUser, "Task overdue", "Fix the bug is overdue", "/backlog/5");
  }

  @Test
  void doesNotSendPush_whenPushServiceNotEnabled() {
    when(pushNotificationService.isEnabled()).thenReturn(false);

    listener.onNotificationCreated(new NotificationCreatedEvent(100L));

    verifyNoInteractions(notificationRepository);
    verify(pushNotificationService, never()).sendNotification(any(), any(), any(), any());
  }

  @Test
  void doesNotSendPush_whenUserOptedOut() {
    when(pushNotificationService.isEnabled()).thenReturn(true);
    when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
    UserPreference preference = UserPreference.builder().user(testUser).pushEnabled(false).build();
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(preference));

    listener.onNotificationCreated(new NotificationCreatedEvent(100L));

    verify(pushNotificationService, never()).sendNotification(any(), any(), any(), any());
  }

  @Test
  void sendsPush_whenUserHasNoPreferenceRow_defaultsToEnabled() {
    when(pushNotificationService.isEnabled()).thenReturn(true);
    when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

    listener.onNotificationCreated(new NotificationCreatedEvent(100L));

    verify(pushNotificationService)
        .sendNotification(testUser, "Task overdue", "Fix the bug is overdue", "/backlog/5");
  }

  @Test
  void doesNotSendPush_whenNotificationNotFound() {
    when(pushNotificationService.isEnabled()).thenReturn(true);
    when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

    listener.onNotificationCreated(new NotificationCreatedEvent(999L));

    verify(pushNotificationService, never()).sendNotification(any(), any(), any(), any());
  }

  @Test
  void swallowsException_whenPushNotificationServiceThrows() {
    when(pushNotificationService.isEnabled()).thenReturn(true);
    when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
    when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("boom"))
        .when(pushNotificationService)
        .sendNotification(any(), any(), any(), any());

    // Should not propagate
    listener.onNotificationCreated(new NotificationCreatedEvent(100L));
  }
}
