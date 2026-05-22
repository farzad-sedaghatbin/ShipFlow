package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Status of the Jira OAuth connection (v1.2.0 S30). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraConnectionStatusDTO {
  /** True if a Jira access token has been stored. */
  private boolean connected;
  /** True if client-id and client-secret are present in environment. */
  private boolean configured;
  /** Atlassian cloud id, null if not connected. */
  private String cloudId;
  /** Atlassian cloud display name, null if not connected. */
  private String cloudName;
}
