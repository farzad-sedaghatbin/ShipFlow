package com.github.farzadsedaghatbin.shipflow.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.dto.admin.EmailStatusDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EmailStatusServiceTest {

  @Test
  void reportsTheServersRealMailSettings_withoutAnyPassword() {
    MockEnvironment env = new MockEnvironment().withProperty("spring.mail.host", "mail.goldorigins.com")
        .withProperty("spring.mail.port", "587").withProperty("spring.mail.username", "shipflow@goldorigins.com")
        .withProperty("spring.mail.password", "super-secret")
        .withProperty("app.mail.from", "GoldOrigins ShipFlow <shipflow@goldorigins.com>")
        .withProperty("app.mail.reply-to", "").withProperty("app.mail.base-url", "https://shipflow.goldorigins.com")
        .withProperty("spring.mail.properties.mail.smtp.starttls.required", "true");

    EmailStatusDTO status = new EmailStatusService(env).status();

    assertThat(status.configured()).isTrue();
    assertThat(status.host()).isEqualTo("mail.goldorigins.com");
    assertThat(status.port()).isEqualTo(587);
    assertThat(status.username()).isEqualTo("shipflow@goldorigins.com");
    assertThat(status.from()).isEqualTo("GoldOrigins ShipFlow <shipflow@goldorigins.com>");
    assertThat(status.replyTo()).isNull();
    assertThat(status.baseUrl()).isEqualTo("https://shipflow.goldorigins.com");
    assertThat(status.starttlsRequired()).isTrue();
    assertThat(status.toString()).doesNotContain("super-secret");
  }

  @Test
  void aBlankHostMeansEmailIsOff() {
    MockEnvironment env = new MockEnvironment().withProperty("spring.mail.host", "");

    EmailStatusDTO status = new EmailStatusService(env).status();

    assertThat(status.configured()).isFalse();
    assertThat(status.host()).isNull();
  }
}
