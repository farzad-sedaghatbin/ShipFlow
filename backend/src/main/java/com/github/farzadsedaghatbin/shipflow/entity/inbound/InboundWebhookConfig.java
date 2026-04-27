package com.github.farzadsedaghatbin.shipflow.entity.inbound;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Persisted configuration for an inbound webhook provider.
 *
 * <p>Admins create / update these from the UI (Integrations → Inbound Webhooks).
 * The {@link com.github.farzadsedaghatbin.shipflow.service.inbound.InboundWebhookRouter}
 * uses them to validate signatures and dispatch events.</p>
 *
 * @since 0.6.0
 */
@Entity
@Table(name = "inbound_webhook_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundWebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique provider key used in the URL: /api/inbound/{providerName} */
    @Column(name = "provider_name", nullable = false, unique = true)
    private String providerName;

    /** Human-readable label shown in the UI (e.g. "Intercom Production") */
    @Column(name = "display_name")
    private String displayName;

    /** Shared secret used for HMAC signature validation */
    @Column(name = "webhook_secret", length = 1000)
    private String webhookSecret;

    /** Signature header name the provider sends (e.g. X-Hub-Signature-256) */
    @Column(name = "signature_header")
    private String signatureHeader;

    /** HMAC algorithm: HmacSHA1, HmacSHA256, etc. */
    @Column(name = "hmac_algorithm")
    private String hmacAlgorithm;

    /** Optional description / notes for admins */
    @Column(name = "description", length = 2000)
    private String description;

    /** Whether this provider is currently active */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isEnabled == null) {
            isEnabled = true;
        }
        if (hmacAlgorithm == null) {
            hmacAlgorithm = "HmacSHA256";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
