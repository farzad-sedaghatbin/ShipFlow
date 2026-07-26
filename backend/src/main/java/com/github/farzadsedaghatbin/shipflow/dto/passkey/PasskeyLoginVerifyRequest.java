package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of {@code POST /api/auth/passkeys/login/verify}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyLoginVerifyRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String credentialId;

  @NotBlank
  private String authenticatorData;

  @NotBlank
  private String clientDataJSON;

  @NotBlank
  private String signature;

  /** Base64url-encoded user handle, if the authenticator returned one. Optional. */
  private String userHandle;
}
