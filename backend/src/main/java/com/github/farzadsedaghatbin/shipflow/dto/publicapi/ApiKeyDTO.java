package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyDTO {

  private Long id;
  private String name;
  private String keyPrefix;
  private Set<ApiKeyScope> scopes;
  private Boolean isActive;
  private LocalDateTime expiresAt;
  private LocalDateTime lastUsedAt;
  private LocalDateTime createdAt;
  private LocalDateTime revokedAt;

  private String createdByUsername;

  /**
   * The raw API key value – only populated on creation response.
   * Never stored in the database.
   */
  private String rawKey;

  /** Null when this key is unrestricted (org-wide access). */
  private Long restrictedToProjectId;

  /**
   * Resolved server-side from {@link #restrictedToProjectId} so the frontend doesn't need a
   * second call to show the project's name. Null/omitted when the key is unrestricted.
   */
  private String restrictedToProjectName;
}
