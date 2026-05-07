package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionDTO {
  private Long id;
  private String name;
  private String targetUrl;
  private String eventTypes;
  private Boolean isActive;
  private Integer failureCount;
  private java.time.LocalDateTime createdAt;
  private java.time.LocalDateTime updatedAt;

  /** The webhook signing secret – only populated on creation response. */
  private String secret;
}
