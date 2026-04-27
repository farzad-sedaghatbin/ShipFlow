package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotificationDTO {
  private Long id;
  private Long userId;
  private String type;
  private String title;
  private String message;
  private String severity;
  private String actionUrl;
  private String entityType;
  private Long entityId;
  private Boolean isRead;
  private LocalDateTime readAt;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;
}
