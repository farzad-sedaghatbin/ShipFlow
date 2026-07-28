package com.github.farzadsedaghatbin.shipflow.dto.push;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Request body for registering a browser's Web Push subscription. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscribeRequest {

  @NotBlank private String endpoint;

  @NotBlank private String p256dhKey;

  @NotBlank private String authKey;

  /** Optional — browser/device user agent, for a future "manage devices" UI. */
  private String userAgent;
}
