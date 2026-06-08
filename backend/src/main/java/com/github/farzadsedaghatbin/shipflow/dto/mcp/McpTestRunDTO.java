package com.github.farzadsedaghatbin.shipflow.dto.mcp;

import com.github.farzadsedaghatbin.shipflow.dto.qa.TestRunDTO;
import lombok.Builder;
import lombok.Data;

/** Compact test-run representation for MCP tool responses. */
@Data
@Builder
public class McpTestRunDTO {

  private Long id;
  private Long testCaseId;
  private String testCaseKey;
  private String testCaseTitle;
  private String status;
  private String executedAt;
  private String executedByName;
  private Integer durationSeconds;
  private String notes;
  private String actualResult;
  private String buildVersion;
  private String environment;
  private Long bugReportId;
  private String bugReportKey;

  public static McpTestRunDTO from(TestRunDTO dto) {
    return McpTestRunDTO.builder()
        .id(dto.getId())
        .testCaseId(dto.getTestCaseId())
        .testCaseKey(dto.getTestCaseKey())
        .testCaseTitle(dto.getTestCaseTitle())
        .status(dto.getStatus() != null ? dto.getStatus().name() : null)
        .executedAt(dto.getExecutedAt() != null ? dto.getExecutedAt().toString() : null)
        .executedByName(dto.getExecutedByName())
        .durationSeconds(dto.getDurationSeconds())
        .notes(dto.getNotes())
        .actualResult(dto.getActualResult())
        .buildVersion(dto.getBuildVersion())
        .environment(dto.getEnvironment())
        .bugReportId(dto.getBugReportId())
        .bugReportKey(dto.getBugReportKey())
        .build();
  }
}
