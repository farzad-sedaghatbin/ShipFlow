package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing dashboard notifications for users. Tracks overdue tasks,
 * blocked scopes, cycle deadlines, and other important events.
 */
@Entity
@Table(name = "dashboard_notifications", indexes = {@Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_read", columnList = "is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardNotification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Type of notification: OVERDUE_TASK, BLOCKED_SCOPE, CYCLE_DEADLINE,
   * PITCH_AT_RISK, HILL_CHART_STALLED, CYCLE_ENDING_SOON, etc.
   */
  @Column(name = "type", nullable = false, length = 50)
  private String type;

  /** Title of the notification */
  @Column(name = "title", nullable = false, length = 255)
  private String title;

  /** Detailed message */
  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  /** Severity level: INFO, WARNING, ERROR, CRITICAL */
  @Column(name = "severity", nullable = false, length = 20)
  private String severity = "INFO";

  /** Link to related entity (e.g., /tasks/123, /pitches/456) */
  @Column(name = "action_url", length = 500)
  private String actionUrl;

  /** Related entity type (TASK, PITCH, CYCLE, SCOPE, etc.) */
  @Column(name = "entity_type", length = 50)
  private String entityType;

  /** Related entity ID */
  @Column(name = "entity_id")
  private Long entityId;

  /** Whether the notification has been read */
  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  /** When the notification was read */
  @Column(name = "read_at")
  private LocalDateTime readAt;

  /** Optional expiration date for auto-dismissal */
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  /** Mark notification as read */
  public void markAsRead() {
    this.isRead = true;
    this.readAt = LocalDateTime.now();
  }
}
