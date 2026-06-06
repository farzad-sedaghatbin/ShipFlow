package com.github.farzadsedaghatbin.shipflow.dto.sso;

import com.github.farzadsedaghatbin.shipflow.entity.ProviderType;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for an IdentityProvider.
 *
 * <p>Note: {@code clientSecret} is intentionally omitted — secrets are never returned to clients.
 */
@Data
@Builder
public class IdentityProviderDTO {

  private Long id;
  private Long organizationId;
  private String name;
  private ProviderType providerType;
  private String clientId;
  private String metadataUrl;
  private String entityId;
  private String ssoUrl;
  /** SAML2 certificate PEM — included for admin inspection; omit from public list endpoint. */
  private String certificate;

  private Boolean isEnabled;
  private Boolean enforceSso;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
