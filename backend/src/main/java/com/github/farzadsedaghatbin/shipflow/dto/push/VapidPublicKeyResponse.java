package com.github.farzadsedaghatbin.shipflow.dto.push;

import lombok.*;

/** The server's VAPID public key, needed by the frontend to create a `PushSubscription`. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VapidPublicKeyResponse {

  private String publicKey;

  /** True when Web Push is actually configured server-side (VAPID keys present). */
  private boolean enabled;
}
