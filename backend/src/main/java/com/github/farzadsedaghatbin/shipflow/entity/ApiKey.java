package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Represents a public API key for external integrations.
 * The raw key is shown only once at creation; we store only the SHA-256 hash.
 */
@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_keys_key_hash", columnList = "key_hash", unique = true),
    @Index(name = "idx_api_keys_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Human-readable label, e.g. "CI/CD pipeline key". */
  @Column(nullable = false)
  private String name;

  /** First 8 chars of the raw key – shown in the UI for identification. */
  @Column(nullable = false, length = 8)
  private String keyPrefix;

  /** SHA-256 hash of the full key. Used for look-ups. */
  @Column(name = "key_hash", nullable = false, unique = true)
  private String keyHash;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Comma-separated scopes (READ, WRITE, ADMIN). */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
  @Column(name = "scope")
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Set<ApiKeyScope> scopes = new HashSet<>();

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column
  private LocalDateTime expiresAt;

  @Column
  private LocalDateTime lastUsedAt;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime revokedAt;

  /**
   * When set, restricts this key to a single project: every {@code /api/v1/public/**} and
   * {@code /api/v1/public/data/**} call authenticated with this key is limited to resources
   * belonging to this project. A plain loose FK-id column (not a {@code @ManyToOne}) since
   * callers only ever need the id to compare against a resource's own project id or to look the
   * project's name up separately — never a loaded {@code Project} association.
   * {@code NULL} means unrestricted (org-wide access), which is the behavior of every key that
   * existed before this field was added.
   */
  @Column(name = "restricted_to_project_id")
  private Long restrictedToProjectId;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public boolean isExpired() {
    return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean isUsable() {
    return Boolean.TRUE.equals(isActive) && !isExpired() && revokedAt == null;
  }
}
