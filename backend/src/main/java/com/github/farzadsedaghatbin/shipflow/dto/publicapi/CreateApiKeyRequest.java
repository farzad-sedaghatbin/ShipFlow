package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

  @NotBlank(message = "API key name is required")
  private String name;

  /** Scopes to grant. Defaults to READ if not supplied. */
  private Set<ApiKeyScope> scopes;

  /** Optional expiration date. Null means never expires. */
  private LocalDateTime expiresAt;

  /**
   * Optional project restriction. When set, this key can only access resources belonging to
   * this project via the public API and data export/import endpoints. Null (the default) means
   * unrestricted — org-wide access, matching the pre-existing behavior of every key.
   */
  private Long restrictedToProjectId;
}
