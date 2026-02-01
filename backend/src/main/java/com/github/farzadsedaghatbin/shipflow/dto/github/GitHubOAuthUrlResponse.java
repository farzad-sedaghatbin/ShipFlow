package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for GitHub App OAuth authorization URL response */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOAuthUrlResponse {

  /** Full URL to redirect the user to GitHub for authorization */
  private String authorizationUrl;

  /** State parameter used for CSRF protection Must be verified in the callback */
  private String state;

  /** URL where user will be redirected after authorization */
  private String callbackUrl;

  /** Instructions for the user */
  private String instructions;
}
