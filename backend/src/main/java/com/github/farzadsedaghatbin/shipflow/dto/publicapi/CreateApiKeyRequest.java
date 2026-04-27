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
}
