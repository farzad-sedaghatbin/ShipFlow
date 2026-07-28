package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Short-lived, single-use server-side state for a WebAuthn ceremony, bridging
 * the "options" and "verify" HTTP round-trips. Mirrors {@link
 * PasswordResetToken}'s shape exactly rather than introducing a new ephemeral
 * store (e.g. Redis).
 */
@Entity
@Table(name = "webauthn_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebAuthnChallenge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Base64url-encoded random challenge value. */
  @Column(nullable = false, unique = true, length = 255)
  private String challenge;

  /**
   * The user this challenge was issued for. Nullable: during login, if the
   * supplied username didn't match any user, we still issue a
   * generically-shaped challenge (to avoid username enumeration) with no
   * associated user.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  /** The username the login flow was started with, regardless of whether it resolved to a user. */
  @Column(name = "username_hint", length = 255)
  private String usernameHint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChallengePurpose purpose;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  @Column(nullable = false)
  private Boolean used;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (used == null) {
      used = false;
    }
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiryDate);
  }

  public boolean isValid() {
    return !used && !isExpired();
  }
}
