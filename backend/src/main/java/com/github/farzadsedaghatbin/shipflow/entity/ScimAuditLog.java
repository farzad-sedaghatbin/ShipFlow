package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/** Audit record for SCIM 2.0 provisioning events. */
@Entity
@Table(name = "scim_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Event type: USER_CREATED, USER_UPDATED, USER_DEACTIVATED, USER_REACTIVATED, USER_DELETED. */
  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  /** External IdP identifier for the affected user. */
  @Column(name = "external_id", length = 500)
  private String externalId;

  /** ShipFlow username of the affected user. */
  @Column(name = "username", length = 255)
  private String username;

  /** Human-readable summary of the change. */
  @Column(name = "details", columnDefinition = "TEXT")
  private String details;

  @Column(name = "occurred_at")
  @Builder.Default
  private OffsetDateTime occurredAt = OffsetDateTime.now();
}
