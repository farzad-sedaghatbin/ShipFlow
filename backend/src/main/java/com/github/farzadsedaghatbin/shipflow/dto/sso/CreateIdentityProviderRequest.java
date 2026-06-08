package com.github.farzadsedaghatbin.shipflow.dto.sso;

import com.github.farzadsedaghatbin.shipflow.entity.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for creating or updating an IdentityProvider. */
@Data
public class CreateIdentityProviderRequest {

  @NotBlank
  private String name;

  @NotNull
  private ProviderType providerType;

  private Long organizationId;

  // OIDC fields
  private String clientId;
  private String clientSecret;
  private String metadataUrl;

  // SAML2 fields
  private String entityId;
  private String ssoUrl;
  private String certificate;

  private Boolean isEnabled;
  private Boolean enforceSso;
}
