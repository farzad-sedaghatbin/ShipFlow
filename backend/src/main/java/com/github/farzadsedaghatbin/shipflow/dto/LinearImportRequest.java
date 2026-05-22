package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for triggering a Linear API import (v1.2.0 S29). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinearImportRequest {
  /** Linear team ID to import from. */
  private String teamId;
  /** Name for the new ShipFlow project. */
  private String projectName;
  /** Project methodology: "KANBAN" or "SCRUM". Defaults to KANBAN if blank. */
  private String projectType;
}
