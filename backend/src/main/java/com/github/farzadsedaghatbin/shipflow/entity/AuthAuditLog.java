package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditEventType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One authentication event: who tried to sign in, from where, on what, and
 * whether it worked.
 *
 * <p>Append-only: nothing updates or deletes rows here. Deliberately not
 * annotated {@code @Audited} — an audit trail does not need an audit trail of
 * its own, and Envers only tracks entities that opt in.
 *
 * <p>{@code username} is stored as supplied rather than as a foreign key. A
 * failed attempt against a non-existent account is one of the more interesting
 * things this table can tell you, and a FK would make it unstorable.
 */
@Entity
@Table(name = "auth_audit_log", indexes = {
    @Index(name = "idx_auth_audit_log_created_at", columnList = "created_at"),
    @Index(name = "idx_auth_audit_log_username", columnList = "username"),
    @Index(name = "idx_auth_audit_log_ip_address", columnList = "ip_address")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private AuthAuditEventType eventType;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome", nullable = false, length = 16)
  private AuthAuditOutcome outcome;

  /** Username exactly as supplied by the caller; may not match any account. */
  @Column(name = "username", length = 255)
  private String username;

  /** Resolved account id, or null when the attempt did not identify one. */
  @Column(name = "user_id")
  private Long userId;

  /** Real client address, resolved through Cloudflare/Caddy. IPv6-capable. */
  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  /** Two-letter country from Cloudflare's CF-IPCountry header, when present. */
  @Column(name = "country", length = 2)
  private String country;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  /** Readable summary derived from the User-Agent, e.g. "Chrome on macOS". */
  @Column(name = "device_summary", length = 160)
  private String deviceSummary;

  /** Why a failure failed. Never contains the attempted password. */
  @Column(name = "failure_reason", length = 160)
  private String failureReason;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
