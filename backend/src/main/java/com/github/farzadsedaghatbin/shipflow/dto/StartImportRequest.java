package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

/** Request body for starting a CSV import. Passed alongside the multipart file. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartImportRequest {

  /** Name for the new Kanban project to create from this import. */
  private String projectName;

  /**
   * Optional format hint: "jira", "linear", "asana", or "auto" (default).
   * When "auto", format is detected from CSV headers.
   */
  private String format;
}
