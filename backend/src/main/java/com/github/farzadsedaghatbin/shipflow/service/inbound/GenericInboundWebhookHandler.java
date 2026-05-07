package com.github.farzadsedaghatbin.shipflow.service.inbound;

import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A generic, DB-driven inbound webhook handler.
 *
 * <p>This handler is <strong>not</strong> registered as a named provider.
 * Instead, it is used by {@link InboundWebhookRouter} as a fallback for
 * any provider that is configured in the database but does not have a
 * dedicated code-level handler.</p>
 *
 * <p>Signature validation uses the HMAC algorithm and secret stored in
 * the {@code inbound_webhook_config} table.</p>
 *
 * @since 0.6.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenericInboundWebhookHandler {

    private final InboundWebhookConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    /**
     * Check whether a DB-configured provider exists and is enabled.
     */
    public boolean supports(String provider) {
        return configRepository.findByProviderName(provider.toLowerCase())
            .map(InboundWebhookConfig::getIsEnabled)
            .orElse(false);
    }

    /**
     * Validate the signature using the DB-stored secret and HMAC algorithm.
     */
    public boolean validateSignature(String provider, String payload, Map<String, String> headers) {
        InboundWebhookConfig config = configRepository.findByProviderName(provider.toLowerCase())
            .orElse(null);
        if (config == null) return false;

        String secret = config.getWebhookSecret();
        // If no secret configured, skip signature validation (allow all)
        if (secret == null || secret.isBlank()) {
            log.debug("No webhook secret configured for provider '{}', skipping signature validation", provider);
            return true;
        }

        String sigHeader = config.getSignatureHeader();
        if (sigHeader == null || sigHeader.isBlank()) {
            sigHeader = "x-hub-signature-256";
        }
        String providedSignature = headers.get(sigHeader.toLowerCase());
        if (providedSignature == null || providedSignature.isBlank()) {
            log.warn("Missing signature header '{}' for provider '{}'", sigHeader, provider);
            return false;
        }

        try {
            String algorithm = config.getHmacAlgorithm() != null
                ? config.getHmacAlgorithm() : "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);

            // Strip common prefixes like "sha256=" or "sha1="
            String stripped = providedSignature.contains("=")
                ? providedSignature.substring(providedSignature.indexOf('=') + 1)
                : providedSignature;

            return computed.equalsIgnoreCase(stripped.trim());
        } catch (Exception e) {
            log.error("Signature validation error for provider '{}': {}", provider, e.getMessage());
            return false;
        }
    }

    /**
     * Generic handler: accepts the payload and returns it as a structured event.
     */
    public Map<String, Object> handle(String provider, String eventType,
        String payload, Map<String, String> headers) {

        Map<String, Object> result = new HashMap<>();
        result.put("status", "accepted");
        result.put("provider", provider);
        result.put("eventType", eventType);

        try {
            Map<String, Object> body = objectMapper.readValue(payload,
                new TypeReference<Map<String, Object>>() {});
            result.put("payload", body);
        } catch (Exception e) {
            log.debug("Could not parse payload as JSON for provider '{}', storing as raw", provider);
            result.put("rawPayload", payload);
        }

        log.info("Generic inbound event accepted: provider={}, eventType={}", provider, eventType);
        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
