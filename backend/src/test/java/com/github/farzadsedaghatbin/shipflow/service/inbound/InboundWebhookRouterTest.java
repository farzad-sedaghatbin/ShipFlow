package com.github.farzadsedaghatbin.shipflow.service.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InboundWebhookRouter}.
 *
 * @since 0.7.0
 */
class InboundWebhookRouterTest {

  private InboundWebhookRouter router;
  private StubHandler activeHandler;
  private StubHandler inactiveHandler;

  @BeforeEach
  void setUp() {
    activeHandler = new StubHandler("test-provider", true, true);
    inactiveHandler = new StubHandler("disabled-provider", false, true);
    router = new InboundWebhookRouter(List.of(activeHandler, inactiveHandler));
  }

  // ── resolve ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("resolve()")
  class ResolveTests {

    @Test
    void resolvesRegisteredProvider() {
      assertThat(router.resolve("test-provider")).isPresent();
    }

    @Test
    void resolveIsCaseInsensitive() {
      assertThat(router.resolve("TEST-PROVIDER")).isPresent();
    }

    @Test
    void returnsEmptyForUnknownProvider() {
      assertThat(router.resolve("nope")).isEmpty();
    }
  }

  // ── listProviders ──────────────────────────────────────────────────────

  @Test
  @DisplayName("listProviders() returns only active providers")
  void listProviders_onlyActive() {
    List<String> active = router.listProviders();
    assertThat(active).containsExactly("test-provider");
    assertThat(active).doesNotContain("disabled-provider");
  }

  // ── route ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("route()")
  class RouteTests {

    @Test
    void successfulRoute() {
      Map<String, Object> result = router.route("test-provider", "event.created",
          "{}", Map.of());
      assertThat(result).containsEntry("status", "success");
    }

    @Test
    void unknownProviderReturnsError() {
      Map<String, Object> result = router.route("unknown", "e", "{}", Map.of());
      assertThat(result)
          .containsEntry("status", "error")
          .extracting("message").asString().contains("Unknown provider");
    }

    @Test
    void inactiveProviderReturnsError() {
      Map<String, Object> result = router.route("disabled-provider", "e", "{}", Map.of());
      assertThat(result)
          .containsEntry("status", "error")
          .extracting("message").asString().contains("not active");
    }

    @Test
    void invalidSignatureReturnsError() {
      activeHandler.signatureValid = false;
      Map<String, Object> result = router.route("test-provider", "e", "{}", Map.of());
      assertThat(result)
          .containsEntry("status", "error")
          .extracting("message").asString().contains("Invalid signature");
    }

    @Test
    void handlerExceptionReturnsError() {
      StubHandler failing = new StubHandler("fail-provider", true, true) {
        @Override
        public Map<String, Object> handle(String eventType, String payload, Map<String, String> headers) {
          throw new RuntimeException("boom");
        }
      };
      InboundWebhookRouter r = new InboundWebhookRouter(List.of(failing));
      Map<String, Object> result = r.route("fail-provider", "e", "{}", Map.of());
      assertThat(result)
          .containsEntry("status", "error")
          .extracting("message").asString().contains("boom");
    }
  }

  // ── stub ───────────────────────────────────────────────────────────────

  private static class StubHandler implements InboundWebhookHandler {
    private final String name;
    private final boolean active;
    boolean signatureValid;

    StubHandler(String name, boolean active, boolean signatureValid) {
      this.name = name;
      this.active = active;
      this.signatureValid = signatureValid;
    }

    @Override
    public String getProviderName() { return name; }

    @Override
    public boolean validateSignature(String payload, Map<String, String> headers) {
      return signatureValid;
    }

    @Override
    public Map<String, Object> handle(String eventType, String payload, Map<String, String> headers) {
      return Map.of("status", "success", "eventType", eventType);
    }

    @Override
    public boolean isActive() { return active; }
  }
}
