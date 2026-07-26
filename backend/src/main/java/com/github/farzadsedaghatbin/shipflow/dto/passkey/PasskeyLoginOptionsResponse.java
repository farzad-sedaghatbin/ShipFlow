package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The {@code PublicKeyCredentialRequestOptions} JSON shape consumed by
 * {@code navigator.credentials.get()}. When the requested username doesn't
 * resolve to a user, {@code allowCredentials} is simply an empty list rather
 * than the endpoint returning 404 — see {@code PasskeyService.beginLogin} for
 * the username-enumeration rationale.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyLoginOptionsResponse {

  /** Base64url-encoded random challenge. */
  private String challenge;

  private String rpId;
  private Long timeout;
  private List<CredentialDescriptor> allowCredentials;

  @Builder.Default
  private String userVerification = "preferred";
}
