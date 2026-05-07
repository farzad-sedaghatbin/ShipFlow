package com.github.farzadsedaghatbin.shipflow.dto.github;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for GitHub App OAuth callback processing result */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOAuthCallbackResult {

  /** Whether the authorization was successful */
  private boolean success;

  /** Installation details if successful */
  private GitHubAppInstallationDTO installation;

  /** Number of repositories synced from this installation */
  private int repositoriesSynced;

  /** List of repository names that were synced */
  private List<String> repositoryNames;

  /** Error message if authorization failed */
  private String error;

  /** Detailed error description */
  private String errorDescription;

  /** URL to redirect the user to after processing */
  private String redirectUrl;

  /** Message to display to the user */
  private String message;
}
