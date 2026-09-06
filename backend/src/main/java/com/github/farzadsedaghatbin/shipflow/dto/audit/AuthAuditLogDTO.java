package com.github.farzadsedaghatbin.shipflow.dto.audit;

import com.github.farzadsedaghatbin.shipflow.entity.AuthAuditLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditEventType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditOutcome;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One authentication event, as returned to administrators. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLogDTO {

  private Long id;
  private AuthAuditEventType eventType;
  private AuthAuditOutcome outcome;
  private String username;
  private Long userId;
  private String ipAddress;
  private String country;
  private String deviceSummary;
  private String userAgent;
  private String failureReason;
  private OffsetDateTime createdAt;

  public static AuthAuditLogDTO from(AuthAuditLog entry) {
    return AuthAuditLogDTO.builder()
        .id(entry.getId())
        .eventType(entry.getEventType())
        .outcome(entry.getOutcome())
        .username(entry.getUsername())
        .userId(entry.getUserId())
        .ipAddress(entry.getIpAddress())
        .country(entry.getCountry())
        .deviceSummary(entry.getDeviceSummary())
        .userAgent(entry.getUserAgent())
        .failureReason(entry.getFailureReason())
        .createdAt(entry.getCreatedAt())
        .build();
  }
}
