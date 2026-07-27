package com.github.farzadsedaghatbin.shipflow.dto.push;

import java.time.OffsetDateTime;
import lombok.*;

/**
 * Acknowledgement returned after subscribing. Deliberately excludes {@code p256dhKey}/
 * {@code authKey} — those are effectively secrets for the owning browser's encryption and must
 * never be echoed back across the controller boundary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscriptionResponse {

  private Long id;
  private String endpoint;
  private OffsetDateTime createdAt;
}
