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

  /**
   * Blank/absent for a discoverable-credential (conditional UI / autofill)
   * login — the account is resolved server-side from {@link #userHandle}
   * instead. See {@code PasskeyService#finishLogin}.
   */
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
