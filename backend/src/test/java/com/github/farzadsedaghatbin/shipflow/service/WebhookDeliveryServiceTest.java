package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

  @Test
  void hmacSha256_shouldProduceConsistentSignature() {
    String sig1 = WebhookDeliveryService.hmacSha256("{\"test\":1}", "secret123");
    String sig2 = WebhookDeliveryService.hmacSha256("{\"test\":1}", "secret123");
    assertThat(sig1).isEqualTo(sig2);
    assertThat(sig1).hasSize(64); // HMAC-SHA256 = 64 hex chars
  }

  @Test
  void hmacSha256_shouldDifferWithDifferentSecrets() {
    String sig1 = WebhookDeliveryService.hmacSha256("{\"test\":1}", "secret1");
    String sig2 = WebhookDeliveryService.hmacSha256("{\"test\":1}", "secret2");
    assertThat(sig1).isNotEqualTo(sig2);
  }

  @Test
  void hmacSha256_shouldDifferWithDifferentPayloads() {
    String sig1 = WebhookDeliveryService.hmacSha256("{\"a\":1}", "secret");
    String sig2 = WebhookDeliveryService.hmacSha256("{\"b\":2}", "secret");
    assertThat(sig1).isNotEqualTo(sig2);
  }
}
