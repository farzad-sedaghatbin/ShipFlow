package com.github.farzadsedaghatbin.shipflow.service.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import org.junit.jupiter.api.Test;

class NoOpPushNotificationServiceTest {

  private final NoOpPushNotificationService service = new NoOpPushNotificationService();

  @Test
  void isEnabled_returnsFalse() {
    assertThat(service.isEnabled()).isFalse();
  }

  @Test
  void getVapidPublicKey_returnsNull() {
    assertThat(service.getVapidPublicKey()).isNull();
  }

  @Test
  void sendNotification_isNoOp() {
    User user = User.builder().id(1L).username("bob").build();
    assertThatCode(() -> service.sendNotification(user, "Title", "Body", "/x")).doesNotThrowAnyException();
  }
}
