package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for GitHub App permissions */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubPermissionsDTO {

  /** Repository contents permission: "read" or "write" */
  private String contents;

  /** Issues permission: "read" or "write" */
  private String issues;

  /** Pull requests permission: "read" or "write" */
  private String pullRequests;

  /** Metadata permission (always "read" for app installations) */
  private String metadata;

  /** Webhooks permission: "read" or "write" */
  private String webhooks;

  /** Checks permission: "read" or "write" */
  private String checks;

  /** Statuses permission: "read" or "write" */
  private String statuses;
}
