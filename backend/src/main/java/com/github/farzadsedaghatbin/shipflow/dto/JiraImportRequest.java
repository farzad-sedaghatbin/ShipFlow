package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for triggering a Jira API import (v1.2.0 S30). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraImportRequest {
  /** Jira project key to import from (e.g. "MYPROJ"). */
  private String projectKey;
  /** Name for the new ShipFlow project. */
  private String projectName;
  /** Project methodology: "KANBAN" or "SCRUM". Defaults to KANBAN if blank. */
  private String projectType;
}
