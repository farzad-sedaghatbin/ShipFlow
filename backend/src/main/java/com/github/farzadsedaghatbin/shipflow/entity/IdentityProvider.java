package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * Represents an external Identity Provider (IdP) that can authenticate users via SAML2 or OIDC.
 *
 * <p>One record per configured IdP per organization. Soft-disabled via {@code isEnabled=false}.
 */
@Entity
@Table(name = "identity_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityProvider {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "organization_id")
  private Long organizationId;

  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider_type", nullable = false, length = 50)
  private ProviderType providerType;

  /** OAuth2 client ID — used for OIDC flows. */
  @Column(name = "client_id", length = 500)
  private String clientId;

  /**
   * OAuth2 client secret — used for OIDC flows.
   *
   * <p>Stored as plain text; callers are responsible for encryption before persisting.
   */
  @Column(name = "client_secret", columnDefinition = "TEXT")
  private String clientSecret;

  /**
   * SAML2 metadata URL (IdP XML metadata endpoint) or OIDC discovery URL
   * ({@code /.well-known/openid-configuration}).
   */
  @Column(name = "metadata_url", columnDefinition = "TEXT")
  private String metadataUrl;

  /** SAML2 entity/issuer ID. */
  @Column(name = "entity_id", columnDefinition = "TEXT")
  private String entityId;

  /** SAML2 SSO endpoint URL. */
  @Column(name = "sso_url", columnDefinition = "TEXT")
  private String ssoUrl;

  /** SAML2 signing certificate in PEM format. */
  @Column(name = "certificate", columnDefinition = "TEXT")
  private String certificate;

  @Column(name = "is_enabled", nullable = false)
  @Builder.Default
  private Boolean isEnabled = false;

  /** When true, local password login is blocked and all users must go through this IdP. */
  @Column(name = "enforce_sso", nullable = false)
  @Builder.Default
  private Boolean enforceSso = false;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
