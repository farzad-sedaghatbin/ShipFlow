package com.github.farzadsedaghatbin.shipflow.dto.sso;

import lombok.Data;

/**
 * Generic callback payload for both OIDC and SAML2 SSO flows.
 *
 * <p>OIDC: {@code code} + {@code state} + {@code idpId} are populated.
 * SAML2: {@code samlResponse} + {@code relayState} are populated.
 */
@Data
public class SsoCallbackRequest {

  // OIDC fields
  private String code;
  private String state;
  private Long idpId;

  // SAML2 fields
  private String samlResponse;
  private String relayState;
}
