package com.github.farzadsedaghatbin.shipflow.service.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the {@link InboundWebhookHandler} interface.
 *
 * <p>Ensures the interface surface is stable and the method signatures
 * match the documented contract.</p>
 *
 * @since 0.6.0
 */
class InboundWebhookHandlerContractTest {

  @Test
  void interface_declaredExpectedMethods() {
    Set<String> methods = Arrays.stream(InboundWebhookHandler.class.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());

    assertThat(methods).containsExactlyInAnyOrder(
        "getProviderName",
        "validateSignature",
        "handle",
        "isActive"
    );
  }

  @Test
  void getProviderName_returnsString() throws NoSuchMethodException {
    Method m = InboundWebhookHandler.class.getMethod("getProviderName");
    assertThat(m.getReturnType()).isEqualTo(String.class);
    assertThat(m.getParameterCount()).isZero();
  }

  @Test
  void validateSignature_acceptsPayloadAndHeaders() throws NoSuchMethodException {
    Method m = InboundWebhookHandler.class.getMethod("validateSignature", String.class, Map.class);
    assertThat(m.getReturnType()).isEqualTo(boolean.class);
    assertThat(m.getParameterCount()).isEqualTo(2);
  }

  @Test
  void handle_acceptsEventTypePayloadHeaders() throws NoSuchMethodException {
    Method m = InboundWebhookHandler.class.getMethod("handle", String.class, String.class, Map.class);
    assertThat(m.getReturnType()).isEqualTo(Map.class);
    assertThat(m.getParameterCount()).isEqualTo(3);
  }

  @Test
  void isActive_returnsBoolean() throws NoSuchMethodException {
    Method m = InboundWebhookHandler.class.getMethod("isActive");
    assertThat(m.getReturnType()).isEqualTo(boolean.class);
    assertThat(m.getParameterCount()).isZero();
  }
}
