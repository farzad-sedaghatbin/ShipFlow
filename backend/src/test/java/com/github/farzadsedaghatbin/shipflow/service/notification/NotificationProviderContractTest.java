package com.github.farzadsedaghatbin.shipflow.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.service.slack.SlackIntegrationService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the {@link NotificationProvider} interface.
 *
 * <p>Verifies that known implementations correctly implement the interface
 * and that the interface surface is stable.</p>
 *
 * @since 0.6.0
 */
class NotificationProviderContractTest {

  @Test
  void slackIntegrationService_implementsNotificationProvider() {
    assertThat(NotificationProvider.class).isAssignableFrom(SlackIntegrationService.class);
  }

  @Test
  void interfaceDeclares_expectedMethods() {
    Set<String> methods = Arrays.stream(NotificationProvider.class.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());

    assertThat(methods).containsExactlyInAnyOrder(
        "getProviderName",
        "sendNotification",
        "isActive"
    );
  }

  @Test
  void sendNotification_hasCorrectSignature() throws NoSuchMethodException {
    Method method = NotificationProvider.class.getMethod("sendNotification",
        String.class, String.class, String.class, String.class, Long.class);
    assertThat(method.getParameterCount()).isEqualTo(5);
    assertThat(method.getReturnType()).isEqualTo(void.class);
  }

  @Test
  void isActive_returnsBoolean() throws NoSuchMethodException {
    Method method = NotificationProvider.class.getMethod("isActive");
    assertThat(method.getReturnType()).isEqualTo(boolean.class);
  }

  @Test
  void getProviderName_returnsString() throws NoSuchMethodException {
    Method method = NotificationProvider.class.getMethod("getProviderName");
    assertThat(method.getReturnType()).isEqualTo(String.class);
  }
}
