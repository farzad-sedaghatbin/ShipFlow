package com.github.farzadsedaghatbin.shipflow.entity;

/** Records how a user account was originally provisioned. */
public enum ProvisionedVia {
  /** Standard local username/password registration. */
  LOCAL,
  /** Provisioned via SAML2 assertion. */
  SAML2,
  /** Provisioned via OpenID Connect token. */
  OIDC,
  /** Provisioned via SCIM 2.0 endpoint. */
  SCIM
}
