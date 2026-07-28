package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The {@code PublicKeyCredentialCreationOptions} JSON shape consumed by the
 * browser's {@code navigator.credentials.create()}. Fields that are binary in
 * the WebAuthn spec (challenge, user.id, excludeCredentials[].id) are
 * base64url-encoded strings here — the frontend's WebAuthn helper decodes
 * them before calling into the browser API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationOptionsResponse {

  private RelyingParty rp;
  private UserInfo user;

  /** Base64url-encoded random challenge. */
  private String challenge;

  private List<PubKeyCredParam> pubKeyCredParams;
  private Long timeout;
  private List<CredentialDescriptor> excludeCredentials;
  private AuthenticatorSelection authenticatorSelection;

  @Builder.Default
  private String attestation = "none";

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RelyingParty {
    private String id;
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UserInfo {
    /** Base64url-encoded user handle (we use the user's DB id as bytes). */
    private String id;
    private String name;
    private String displayName;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PubKeyCredParam {
    @Builder.Default
    private String type = "public-key";
    private long alg;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AuthenticatorSelection {
    private String residentKey;
    private String userVerification;
  }
}
