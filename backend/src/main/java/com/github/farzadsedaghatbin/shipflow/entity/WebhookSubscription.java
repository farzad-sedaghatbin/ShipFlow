package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Stores outgoing webhook subscriptions.
 * When a matching domain event fires, ShipFlow sends a signed POST to the target URL.
 */
@Entity
@Table(name = "webhook_subscriptions", indexes = {
    @Index(name = "idx_webhook_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Human-readable name, e.g. "Slack notifier". */
  @Column(nullable = false)
  private String name;

  /** The URL that will receive POST requests. */
  @Column(nullable = false)
  private String targetUrl;

  /** HMAC-SHA256 secret used to sign payloads. */
  @Column(nullable = false)
  private String secret;

  /**
   * Comma-separated event types this subscription listens for.
   * Use "*" for all events.
   * Examples: "task.status_changed,pitch.status_changed,cycle.status_changed,task.completed,release.published"
   */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String eventTypes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  /** Count of consecutive delivery failures (reset on success). */
  @Column(nullable = false)
  @Builder.Default
  private Integer failureCount = 0;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * Returns true if this subscription should receive an event of the given type.
   */
  public boolean matchesEvent(String eventType) {
    if ("*".equals(eventTypes)) {
      return true;
    }
    for (String et : eventTypes.split(",")) {
      if (et.trim().equalsIgnoreCase(eventType)) {
        return true;
      }
    }
    return false;
  }
}
