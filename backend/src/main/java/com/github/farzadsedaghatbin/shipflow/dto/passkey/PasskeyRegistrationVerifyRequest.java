package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/passkeys/register/verify}. Flattened rather than
 * nesting the browser's raw {@code PublicKeyCredential} response shape
 * (id/rawId/type/response.*) — the frontend task can map from
 * {@code navigator.credentials.create()}'s result into this shape.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationVerifyRequest {

  /** User-editable label, e.g. "MacBook Touch ID". Defaults applied server-side if blank. */
  private String deviceName;

  @NotBlank
  private String credentialId;

  @NotBlank
  private String clientDataJSON;

  @NotBlank
  private String attestationObject;

  private List<String> transports;
}
