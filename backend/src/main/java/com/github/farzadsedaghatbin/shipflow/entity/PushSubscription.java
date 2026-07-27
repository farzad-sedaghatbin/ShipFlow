package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * A single browser Web Push subscription (endpoint + RFC 8291 encryption keys) registered by the
 * frontend service worker ({@code frontend/src/sw.ts}). A user may have several rows — one per
 * browser/device that opted in.
 */
@Entity
@Table(
    name = "push_subscriptions",
    indexes = {@Index(name = "idx_push_subscriptions_user", columnList = "user_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** The push service endpoint URL. Globally unique per browser subscription. */
  @Column(name = "endpoint", nullable = false, unique = true, columnDefinition = "TEXT")
  private String endpoint;

  /** The subscription's public key (base64url), used to encrypt the push payload. */
  @Column(name = "p256dh_key", nullable = false, columnDefinition = "TEXT")
  private String p256dhKey;

  /** The subscription's auth secret (base64url), used to encrypt the push payload. */
  @Column(name = "auth_key", nullable = false, columnDefinition = "TEXT")
  private String authKey;

  /** Browser/device user agent at subscription time, for display in a "manage devices" UI. */
  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "last_used_at")
  private OffsetDateTime lastUsedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}
