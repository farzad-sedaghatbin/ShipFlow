package com.github.farzadsedaghatbin.shipflow.service.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GenericInboundWebhookHandler}.
 *
 * @since 0.6.0
 */
class GenericInboundWebhookHandlerTest {

    private InboundWebhookConfigRepository configRepository;
    private GenericInboundWebhookHandler handler;

    @BeforeEach
    void setUp() {
        configRepository = mock(InboundWebhookConfigRepository.class);
        handler = new GenericInboundWebhookHandler(configRepository, new ObjectMapper());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private InboundWebhookConfig enabledConfig(String provider, String secret) {
        InboundWebhookConfig c = new InboundWebhookConfig();
        c.setProviderName(provider);
        c.setIsEnabled(true);
        c.setWebhookSecret(secret);
        c.setSignatureHeader("x-hub-signature-256");
        c.setHmacAlgorithm("HmacSHA256");
        return c;
    }

    /** Compute a real HmacSHA256 over payload with the given secret. */
    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── supports ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("supports()")
    class SupportsTests {

        @Test
        @DisplayName("returns true when provider is in DB and enabled")
        void trueWhenEnabledInDB() {
            InboundWebhookConfig c = new InboundWebhookConfig();
            c.setIsEnabled(true);
            when(configRepository.findByProviderName("zendesk"))
                .thenReturn(Optional.of(c));

            assertThat(handler.supports("zendesk")).isTrue();
        }

        @Test
        @DisplayName("returns false when provider is in DB but disabled")
        void falseWhenDisabledInDB() {
            InboundWebhookConfig c = new InboundWebhookConfig();
            c.setIsEnabled(false);
            when(configRepository.findByProviderName("zendesk"))
                .thenReturn(Optional.of(c));

            assertThat(handler.supports("zendesk")).isFalse();
        }

        @Test
        @DisplayName("returns false when provider is not in DB")
        void falseWhenNotInDB() {
            when(configRepository.findByProviderName("unknown"))
                .thenReturn(Optional.empty());

            assertThat(handler.supports("unknown")).isFalse();
        }

        @Test
        @DisplayName("normalises provider name to lowercase")
        void normalisesProvider() {
            InboundWebhookConfig c = new InboundWebhookConfig();
            c.setIsEnabled(true);
            when(configRepository.findByProviderName("zendesk"))
                .thenReturn(Optional.of(c));

            // Mixed-case input should still resolve
            assertThat(handler.supports("ZENDESK")).isTrue();
        }
    }

    // ── validateSignature ────────────────────────────────────────────

    @Nested
    @DisplayName("validateSignature()")
    class ValidateSignatureTests {

        @Test
        @DisplayName("returns true when HMAC matches exactly")
        void trueOnValidHmac() throws Exception {
            String secret = "my-webhook-secret";
            String payload = "{\"event\":\"test\"}";
            String hex = hmacSha256Hex(secret, payload);

            InboundWebhookConfig config = enabledConfig("provider", secret);
            when(configRepository.findByProviderName("provider"))
                .thenReturn(Optional.of(config));

            Map<String, String> headers = new HashMap<>();
            headers.put("x-hub-signature-256", hex);

            assertThat(handler.validateSignature("provider", payload, headers)).isTrue();
        }

        @Test
        @DisplayName("returns true when HMAC matches with sha256= prefix stripped")
        void trueWithPrefixStripped() throws Exception {
            String secret = "my-webhook-secret";
            String payload = "{\"event\":\"test\"}";
            String hex = "sha256=" + hmacSha256Hex(secret, payload);

            InboundWebhookConfig config = enabledConfig("provider", secret);
            when(configRepository.findByProviderName("provider"))
                .thenReturn(Optional.of(config));

            Map<String, String> headers = new HashMap<>();
            headers.put("x-hub-signature-256", hex);

            assertThat(handler.validateSignature("provider", payload, headers)).isTrue();
        }

        @Test
        @DisplayName("returns false when HMAC does not match")
        void falseOnWrongHmac() {
            InboundWebhookConfig config = enabledConfig("provider", "correct-secret");
            when(configRepository.findByProviderName("provider"))
                .thenReturn(Optional.of(config));

            Map<String, String> headers = new HashMap<>();
            headers.put("x-hub-signature-256", "deadbeefcafe0000");

            assertThat(handler.validateSignature("provider", "{}", headers)).isFalse();
        }

        @Test
        @DisplayName("returns false when signature header is missing")
        void falseWhenHeaderMissing() {
            InboundWebhookConfig config = enabledConfig("provider", "my-secret");
            when(configRepository.findByProviderName("provider"))
                .thenReturn(Optional.of(config));

            // Empty headers — signature header not present
            assertThat(handler.validateSignature("provider", "{}", Map.of())).isFalse();
        }

        @Test
        @DisplayName("returns true (skip validation) when no secret is configured")
        void trueWhenNoSecretConfigured() {
            InboundWebhookConfig config = enabledConfig("provider", null);
            when(configRepository.findByProviderName("provider"))
                .thenReturn(Optional.of(config));

            assertThat(handler.validateSignature("provider", "{}", Map.of())).isTrue();
        }

        @Test
        @DisplayName("returns false when provider config not found")
        void falseWhenConfigNotFound() {
            when(configRepository.findByProviderName("ghost"))
                .thenReturn(Optional.empty());

            assertThat(handler.validateSignature("ghost", "{}", Map.of())).isFalse();
        }
    }

    // ── handle ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("handle()")
    class HandleTests {

        @Test
        @DisplayName("returns accepted status for valid JSON payload")
        void acceptsJsonPayload() {
            Map<String, Object> result = handler.handle("pagerduty", "alert.triggered",
                "{\"severity\":\"critical\"}", Map.of());

            assertThat(result).containsEntry("status", "accepted");
            assertThat(result).containsEntry("provider", "pagerduty");
            assertThat(result).containsEntry("eventType", "alert.triggered");
            assertThat(result).containsKey("payload");
        }

        @Test
        @DisplayName("stores raw payload when body is not JSON")
        void rawPayloadForNonJson() {
            Map<String, Object> result = handler.handle("custom", "ping",
                "plain-text-body", Map.of());

            assertThat(result).containsEntry("status", "accepted");
            assertThat(result).containsKey("rawPayload");
            assertThat(result).doesNotContainKey("payload");
        }

        @Test
        @DisplayName("includes provider and eventType in all responses")
        void alwaysIncludesProviderAndEvent() {
            Map<String, Object> result = handler.handle("zendesk", "ticket.created",
                "{}", Map.of());

            assertThat(result).containsEntry("provider", "zendesk");
            assertThat(result).containsEntry("eventType", "ticket.created");
        }
    }
}
