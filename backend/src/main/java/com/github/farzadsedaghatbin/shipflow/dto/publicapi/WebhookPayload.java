package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.*;

/**
 * Standard webhook payload sent to subscriber endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookPayload {
  private String id;
  private String eventType;
  private LocalDateTime timestamp;
  private Map<String, Object> data;
}
