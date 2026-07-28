package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * A single registered WebAuthn authenticator (passkey) for a {@link User}. A
 * user may have several of these (one per device/authenticator).
 *
 * <p>{@code publicKeyCose} stores the full serialized {@code
 * AttestedCredentialData} blob (AAGUID + credential id + COSE public key) as
 * produced by webauthn4j's own {@code AttestedCredentialDataConverter} — this
 * is the official round-trip serialization used to reconstruct the
 * authenticator for signature verification at login time, not a hand-rolled
 * format.
 */
@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredential {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Base64url-encoded WebAuthn credential id. Unique — used to look up the credential at login time. */
  @Column(name = "credential_id", nullable = false, unique = true, length = 500)
  private String credentialId;

  /** Base64-encoded, webauthn4j-serialized AttestedCredentialData (AAGUID + credential id + COSE key). */
  @Column(name = "public_key_cose", nullable = false, columnDefinition = "TEXT")
  private String publicKeyCose;

  @Column(name = "sign_count", nullable = false)
  @Builder.Default
  private Long signCount = 0L;

  @Column(name = "attestation_type", length = 50)
  private String attestationType;

  /** Comma-joined transports, e.g. "internal,hybrid". */
  @Column(name = "transports", length = 255)
  private String transports;

  /** User-editable label, e.g. "MacBook Touch ID". */
  @Column(name = "device_name", length = 100)
  private String deviceName;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_used_at")
  private LocalDateTime lastUsedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (signCount == null) {
      signCount = 0L;
    }
  }
}
