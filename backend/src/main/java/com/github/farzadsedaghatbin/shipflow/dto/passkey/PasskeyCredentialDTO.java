package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A registered passkey as shown to its owner. Never carries the public key or
 * credential id bytes — those never cross the controller boundary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredentialDTO {

  private Long id;
  private String deviceName;
  private LocalDateTime createdAt;
  private LocalDateTime lastUsedAt;
  private List<String> transports;
}
