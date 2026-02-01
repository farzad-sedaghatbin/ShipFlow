package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/** DTO for QA test management feature status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QATestManagementStatusDTO {
  private Boolean testManagementEnabled;
  private Boolean aiTestGenerationEnabled;
  private Long totalTestCases;
  private Long totalBugReports;
  private Long totalTestRuns;
  private Long aiGeneratedTestCases;
}
