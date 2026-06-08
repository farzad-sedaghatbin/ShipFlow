package com.github.farzadsedaghatbin.shipflow.dto.sso;

import lombok.Builder;
import lombok.Data;

/** Returned when the frontend asks ShipFlow to begin an SSO flow. */
@Data
@Builder
public class SsoInitiateResponse {

  /** The URL the browser should redirect to in order to reach the IdP. */
  private String redirectUrl;

  /** Opaque state token stored server-side in Redis (TTL 5 min). */
  private String state;
}
