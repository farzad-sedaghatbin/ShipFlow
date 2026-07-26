package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A {@code PublicKeyCredentialDescriptor} as expected by
 * {@code navigator.credentials.create()}'s {@code excludeCredentials} and
 * {@code navigator.credentials.get()}'s {@code allowCredentials}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialDescriptor {

  /** Base64url-encoded credential id. */
  private String id;

  @Builder.Default
  private String type = "public-key";

  private List<String> transports;
}
