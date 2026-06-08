package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A Jira project returned from the accessible-projects endpoint (v1.2.0 S30). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraProjectDTO {
  /** Jira internal project id. */
  private String id;
  /** Jira project key (e.g. "MYPROJ"). */
  private String key;
  /** Display name of the project. */
  private String name;
  /** Optional project description. */
  private String description;
}
