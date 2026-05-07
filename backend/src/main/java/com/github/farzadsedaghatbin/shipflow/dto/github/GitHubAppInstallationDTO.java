package com.github.farzadsedaghatbin.shipflow.dto.github;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for GitHub App Installation details */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubAppInstallationDTO {

  private Long id;
  private Long installationId;
  private String accountType;
  private String accountLogin;
  private String repositorySelection;
  private Boolean isActive;
  private Boolean webhooksConfigured;
  private LocalDateTime lastSyncAt;
  private Integer repositoryCount;
  private LocalDateTime createdAt;
  private String installedByLogin;

  /**
   * List of repositories accessible through this installation Only populated when
   * full details are requested
   */
  private List<GitHubRepositoryDTO> repositories;

  /** Permissions granted to this installation */
  private GitHubPermissionsDTO permissions;
}
